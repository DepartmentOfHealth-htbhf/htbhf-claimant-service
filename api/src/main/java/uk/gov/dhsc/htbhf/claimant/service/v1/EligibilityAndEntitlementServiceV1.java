package uk.gov.dhsc.htbhf.claimant.service.v1;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.CycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityClient;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.Optional;

import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildWithStatus;

@Service
@AllArgsConstructor
public class EligibilityAndEntitlementServiceV1 {

    private final EligibilityClient client;
    private final EligibilityStatusCalculatorV1 eligibilityStatusCalculator;
    private final ClaimRepository claimRepository;
    private final CycleEntitlementCalculator cycleEntitlementCalculator;

    /**
     * Determines the eligibility and entitlement for the given new claimant. If the claimant's NINO is not found in the database,
     * the external eligibility service is called.
     * Claimants determined to be eligible by the external eligibility service must still either be pregnant or have children under 4,
     * otherwise they will be ineligible.
     *
     * @param claimant the claimant to check the eligibility for
     * @return the eligibility and entitlement for the claimant
     */
    public EligibilityAndEntitlementDecision evaluateNewClaimant(Claimant claimant) {
        if (!claimRepository.findLiveClaimsWithNino(claimant.getNino()).isEmpty()) {
            // TODO: MGS: check eligibility first and replace duplicate status with existing claim id. HTBHF-1192
            return buildWithStatus(EligibilityStatus.DUPLICATE);
        }
        EligibilityResponse eligibilityResponse = checkEligibilityForNewClaimant(claimant);
        PaymentCycleVoucherEntitlement entitlement = cycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                LocalDate.now());
        return buildDecision(eligibilityResponse, entitlement);
    }

    /**
     * Determines the eligibility and entitlement for the given existing claimant. No check is made on the NINO as they already exist in the
     * database. The eligibility status is checked by calling the external service.
     * Claimants determined to be eligible by the external eligibility service must still either be pregnant or have children under 4,
     * otherwise they will be ineligible.
     *
     * @param claimant the claimant to check the eligibility for
     * @param cycleStartDate the start date of the payment cycle
     * @param previousCycle the previous payment cycle
     * @return the eligibility and entitlement for the claimant
     */
    public EligibilityAndEntitlementDecision evaluateExistingClaimant(
            Claimant claimant,
            LocalDate cycleStartDate,
            PaymentCycle previousCycle) {
        EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
        PaymentCycleVoucherEntitlement entitlement = cycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        return buildDecision(eligibilityResponse, entitlement);
    }

    private EligibilityResponse checkEligibilityForNewClaimant(Claimant claimant) {
        EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
        EligibilityStatus eligibilityStatus = eligibilityStatusCalculator.determineEligibilityStatusForNewClaim(eligibilityResponse);
        return eligibilityResponse.toBuilder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }

    private EligibilityAndEntitlementDecision buildDecision(EligibilityResponse eligibilityResponse, PaymentCycleVoucherEntitlement entitlement) {
        EligibilityStatus eligibilityStatus = determineEligibilityStatus(eligibilityResponse, entitlement);
        return EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(eligibilityStatus)
                .voucherEntitlement(entitlement)
                .dateOfBirthOfChildren(eligibilityResponse.getDateOfBirthOfChildren())
                .dwpHouseholdIdentifier(eligibilityResponse.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(eligibilityResponse.getHmrcHouseholdIdentifier())
                .build();
    }

    private EligibilityStatus determineEligibilityStatus(EligibilityResponse response, PaymentCycleVoucherEntitlement voucherEntitlement) {
        if (response.getEligibilityStatus() == EligibilityStatus.ELIGIBLE && voucherEntitlement.getTotalVoucherEntitlement() == 0) {
            return EligibilityStatus.INELIGIBLE;
        }
        return response.getEligibilityStatus();
    }

}
