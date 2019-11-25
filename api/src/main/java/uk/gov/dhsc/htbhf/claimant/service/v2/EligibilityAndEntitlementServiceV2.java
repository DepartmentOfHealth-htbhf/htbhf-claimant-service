package uk.gov.dhsc.htbhf.claimant.service.v2;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.DuplicateClaimChecker;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementDecisionFactory;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.v2.IdentityAndEligibilityResponseFactory.fromEligibilityResponse;

@Service
@AllArgsConstructor
@Slf4j
public class EligibilityAndEntitlementServiceV2 implements EligibilityAndEntitlementService {

    private final EligibilityClient client;
    private final DuplicateClaimChecker duplicateClaimChecker;
    private final ClaimRepository claimRepository;
    private final PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;
    private final EligibilityAndEntitlementDecisionFactory decisionFactory;

    /**
     * Determines the eligibility and entitlement for the given new claimant. If the claimant's NINO is not found in the database,
     * the external eligibility service is called.
     * Claimants determined to be eligible by the external eligibility service must still either be pregnant or have children under 4,
     * otherwise they will be ineligible.
     *
     * @param claimant the claimant to check the eligibility for
     * @return the eligibility and entitlement for the claimant
     */
    @Override
    public EligibilityAndEntitlementDecision evaluateNewClaimant(Claimant claimant) {
        log.debug("Looking for live claims for the given NINO");
        Optional<UUID> liveClaimsWithNino = claimRepository.findLiveClaimWithNino(claimant.getNino());
        log.debug("Checking eligibility");
        EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
        log.debug("Calculating entitlement");
        PaymentCycleVoucherEntitlement entitlement = paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                LocalDate.now());
        if (liveClaimsWithNino.isPresent()) {
            return buildDecisionForClaimantAlreadyOnScheme(liveClaimsWithNino.get(), eligibilityResponse, entitlement);
        }
        boolean duplicateHouseholdIdentifierFound = duplicateClaimChecker.liveClaimExistsForHousehold(eligibilityResponse);
        return buildDecisionConsideringDuplicates(eligibilityResponse, entitlement, duplicateHouseholdIdentifierFound);
    }

    /**
     * Determines the eligibility and entitlement for the given existing claimant. No check is made on the NINO as they already exist in the
     * database. The eligibility status is checked by calling the external service.
     * Claimants determined to be eligible by the external eligibility service must still either be pregnant or have children under 4,
     * otherwise they will be ineligible.
     *
     * @param claimant       the claimant to check the eligibility for
     * @param cycleStartDate the start date of the payment cycle
     * @param previousCycle  the previous payment cycle
     * @return the eligibility and entitlement for the claimant
     */
    @Override
    public EligibilityAndEntitlementDecision evaluateClaimantForPaymentCycle(Claimant claimant,
                                                                             LocalDate cycleStartDate,
                                                                             PaymentCycle previousCycle) {
        EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
        PaymentCycleVoucherEntitlement entitlement = paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        return buildDecisionForPaymentCycle(eligibilityResponse, entitlement);
    }

    private EligibilityAndEntitlementDecision buildDecisionForPaymentCycle(EligibilityResponse eligibilityResponse,
                                                                           PaymentCycleVoucherEntitlement entitlement) {
        return buildDecisionConsideringDuplicates(eligibilityResponse, entitlement, false);
    }

    private EligibilityAndEntitlementDecision buildDecisionConsideringDuplicates(EligibilityResponse eligibilityResponse,
                                                                                 PaymentCycleVoucherEntitlement entitlement,
                                                                                 boolean duplicateHouseholdIdentifierFound) {
        return decisionFactory.buildDecision(fromEligibilityResponse(eligibilityResponse),
                entitlement,
                null,
                eligibilityResponse.getHmrcHouseholdIdentifier(),
                duplicateHouseholdIdentifierFound);
    }

    private EligibilityAndEntitlementDecision buildDecisionForClaimantAlreadyOnScheme(UUID existingClaimId,
                                                                                      EligibilityResponse eligibilityResponse,
                                                                                      PaymentCycleVoucherEntitlement entitlement) {
        return decisionFactory.buildDecision(fromEligibilityResponse(eligibilityResponse),
                entitlement,
                existingClaimId,
                eligibilityResponse.getHmrcHouseholdIdentifier(),
                false);
    }

}
