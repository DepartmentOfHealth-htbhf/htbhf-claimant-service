package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.dwp.model.*;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildDuplicateDecisionWithExistingClaimId;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_MATCHED;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_SUPPLIED;

@Primary
@Service
@AllArgsConstructor
@Slf4j
public class EligibilityAndEntitlementService {

    private final EligibilityClient client;
    private final DuplicateClaimChecker duplicateClaimChecker;
    private final ClaimRepository claimRepository;
    private final PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;
    private final EligibilityAndEntitlementDecisionFactory eligibilityAndEntitlementDecisionFactory;

    /**
     * Determines the eligibility and entitlement for the given new claimant using v3 of the service. If the claimant's NINO is not found in the database,
     * the external eligibility service is called.
     * Claimants determined to be eligible by the external eligibility service must still either be pregnant or have children under 4,
     * otherwise they will be ineligible.
     *
     * @param claimant                   the claimant to check the eligibility for
     * @param eligibilityOverride to override the eligibility outcome
     * @return the eligibility and entitlement for the claimant
     */
    public EligibilityAndEntitlementDecision evaluateNewClaimant(Claimant claimant, EligibilityOverride eligibilityOverride) {
        log.debug("Looking for live claims for the given NINO");
        if (StringUtils.isNotEmpty(claimant.getNino())) {
            Optional<UUID> liveClaimsWithNino = claimRepository.findLiveClaimWithNino(claimant.getNino());
            if (liveClaimsWithNino.isPresent()) {
                return buildDuplicateDecisionWithExistingClaimId(liveClaimsWithNino.get());
            }
        }

        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse
                = getCombinedIdentityAndEligibilityResponse(claimant, eligibilityOverride, LocalDate.now());
        PaymentCycleVoucherEntitlement entitlement = paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                identityAndEligibilityResponse.getDobOfChildrenUnder4(),
                LocalDate.now(),
                identityAndEligibilityResponse.getQualifyingReason());
        boolean duplicateHouseholdIdentifierFound = duplicateClaimChecker.liveClaimExistsForHousehold(identityAndEligibilityResponse);
        return eligibilityAndEntitlementDecisionFactory.buildDecision(identityAndEligibilityResponse,
                entitlement, duplicateHouseholdIdentifierFound);
    }

    /**
     * Determines the eligibility and entitlement for the given existing claimant using v3 of the service. No check is made on the NINO as they already exist
     * in the database. The eligibility status is checked by calling the external service.
     * Claimants determined to be eligible by the external eligibility service must still either be pregnant or have children under 4,
     * otherwise they will be ineligible.
     *
     * @param claim          the claim to check eligibility for
     * @param cycleStartDate the start date of the payment cycle
     * @param previousCycle  the previous payment cycle
     * @return the eligibility and entitlement for the claimant
     */
    public EligibilityAndEntitlementDecision evaluateClaimantForPaymentCycle(Claim claim,
                                                                             LocalDate cycleStartDate,
                                                                             PaymentCycle previousCycle) {
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse
                = getCombinedIdentityAndEligibilityResponse(claim.getClaimant(), claim.getEligibilityOverride(), cycleStartDate);
        PaymentCycleVoucherEntitlement entitlement = paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claim.getClaimant().getExpectedDeliveryDate()),
                identityAndEligibilityResponse.getDobOfChildrenUnder4(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement(),
                identityAndEligibilityResponse.getQualifyingReason());
        return eligibilityAndEntitlementDecisionFactory.buildDecision(identityAndEligibilityResponse,
                entitlement, false);
    }

    private CombinedIdentityAndEligibilityResponse getCombinedIdentityAndEligibilityResponse(Claimant claimant,
                                                                                             EligibilityOverride eligibilityOverride,
                                                                                             LocalDate eligibleAtDate) {
        if (isOverride(eligibilityOverride, eligibleAtDate)) {
            VerificationOutcome verificationOutcome = getVerificationOutcome(eligibilityOverride);
            return buildOverrideResponse(eligibilityOverride, claimant, verificationOutcome, IdentityOutcome.MATCHED);
        }

        if (StringUtils.isEmpty(claimant.getNino())) {
            return buildOverrideResponse(null, claimant, NOT_MATCHED, IdentityOutcome.NOT_MATCHED);
        }

        return client.checkIdentityAndEligibility(claimant);
    }

    private boolean isOverride(EligibilityOverride eligibilityOverride, LocalDate eligibleAtDate) {
        return eligibilityOverride != null && eligibleAtDate.isBefore(eligibilityOverride.getOverrideUntil());
    }

    private CombinedIdentityAndEligibilityResponse buildOverrideResponse(EligibilityOverride eligibilityOverride,
                                                                         Claimant claimant,
                                                                         VerificationOutcome verificationOutcome,
                                                                         IdentityOutcome identityOutcome) {

        return getCombinedIdentityAndEligibilityResponseBuilder(eligibilityOverride)
                .identityStatus(identityOutcome)
                .pregnantChildDOBMatch(verificationOutcome)
                .addressLine1Match(verificationOutcome)
                .emailAddressMatch(claimant.getEmailAddress() == null ? NOT_SUPPLIED : verificationOutcome)
                .mobilePhoneMatch(claimant.getPhoneNumber() == null ? NOT_SUPPLIED : verificationOutcome)
                .postcodeMatch(verificationOutcome)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .build();
    }

    private CombinedIdentityAndEligibilityResponse.CombinedIdentityAndEligibilityResponseBuilder getCombinedIdentityAndEligibilityResponseBuilder(
            EligibilityOverride eligibilityOverride) {

        if (eligibilityOverride == null) {
            return CombinedIdentityAndEligibilityResponse.builder()
                    .eligibilityStatus(EligibilityOutcome.NOT_CONFIRMED)
                    .dobOfChildrenUnder4(Collections.emptyList())
                    .qualifyingReason(QualifyingReason.NOT_SET);
        }

        return CombinedIdentityAndEligibilityResponse.builder()
                .eligibilityStatus(eligibilityOverride.getEligibilityOutcome())
                .dobOfChildrenUnder4(eligibilityOverride.getChildrenDob())
                .qualifyingReason(eligibilityOverride.getQualifyingReason());
    }

    private VerificationOutcome getVerificationOutcome(EligibilityOverride eligibilityOverride) {
        return eligibilityOverride.getEligibilityOutcome() == EligibilityOutcome.CONFIRMED
                ? VerificationOutcome.MATCHED
                : VerificationOutcome.NOT_SET;
    }

}
