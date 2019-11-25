package uk.gov.dhsc.htbhf.claimant.service.v3;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.DuplicateClaimChecker;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementDecisionFactory;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Slf4j
public class EligibilityAndEntitlementServiceV3 implements EligibilityAndEntitlementService {

    private final EligibilityClientV3 client;
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
     * @param claimant the claimant to check the eligibility for
     * @return the eligibility and entitlement for the claimant
     */
    @Override
    public EligibilityAndEntitlementDecision evaluateNewClaimant(Claimant claimant) {
        log.debug("Looking for live claims for the given NINO");
        Optional<UUID> liveClaimsWithNino = claimRepository.findLiveClaimWithNino(claimant.getNino());
        IdentityAndEligibilityResponse identityAndEligibilityResponse = client.checkIdentityAndEligibility(claimant);
        PaymentCycleVoucherEntitlement entitlement = paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                identityAndEligibilityResponse.getDobOfChildrenUnder4(),
                LocalDate.now());
        if (liveClaimsWithNino.isPresent()) {
            return eligibilityAndEntitlementDecisionFactory.buildDecision(identityAndEligibilityResponse,
                    entitlement, liveClaimsWithNino.get(), null, false);
        }
        String householdIdentifier = identityAndEligibilityResponse.getHouseholdIdentifier();
        boolean duplicateHouseholdIdentifierFound = duplicateClaimChecker.liveClaimExistsForDwpHousehold(householdIdentifier);
        return eligibilityAndEntitlementDecisionFactory.buildDecision(identityAndEligibilityResponse,
                entitlement, null, null, duplicateHouseholdIdentifierFound);
    }

    /**
     * Determines the eligibility and entitlement for the given existing claimant using v3 of the service. No check is made on the NINO as they already exist
     * in the database. The eligibility status is checked by calling the external service.
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
        IdentityAndEligibilityResponse identityAndEligibilityResponse = client.checkIdentityAndEligibility(claimant);
        PaymentCycleVoucherEntitlement entitlement = paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                identityAndEligibilityResponse.getDobOfChildrenUnder4(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        return eligibilityAndEntitlementDecisionFactory.buildDecision(identityAndEligibilityResponse,
                entitlement, null, null, false);
    }

}
