package uk.gov.dhsc.htbhf.claimant.service;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.dwp.model.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.v3.EligibilityOutcomeToEligibilityStatusConverter.fromEligibilityOutcome;

/**
 * Responsible for building {@link uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision}s based
 * on the provided response object from DWP API call.
 */
@Component
public class EligibilityAndEntitlementDecisionFactory {

    /**
     * Builds a decision based on the given data. This will also store the existing claim id on the decision
     * which is indicative of an ineligible claim as there is already a matching live claim in the database.
     * We only return and store a voucher entitlement if the Claimant is ELIGIBLE which is determined from the {@link IdentityAndEligibilityResponse}
     *
     * @param identityAndEligibilityResponse    The response from DWP.
     * @param entitlement                       The already calculated entitlement.
     * @param existingClaimId                   The matching live claim id from the db.
     * @param duplicateHouseholdIdentifierFound Has a duplicate household identifier been found or not
     * @return The built decision.
     */
    public EligibilityAndEntitlementDecision buildDecision(CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse,
                                                           PaymentCycleVoucherEntitlement entitlement,
                                                           UUID existingClaimId,
                                                           boolean duplicateHouseholdIdentifierFound) {
        EligibilityStatus eligibilityStatus = determineEligibilityStatus(identityAndEligibilityResponse, entitlement, duplicateHouseholdIdentifierFound);
        PaymentCycleVoucherEntitlement voucherEntitlement = determinePaymentCycleVoucherEntitlementFromStatus(entitlement, eligibilityStatus);
        return EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(eligibilityStatus)
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .voucherEntitlement(voucherEntitlement)
                .dateOfBirthOfChildren(identityAndEligibilityResponse.getDobOfChildrenUnder4())
                .dwpHouseholdIdentifier(identityAndEligibilityResponse.getDwpHouseholdIdentifier())
                .existingClaimId(existingClaimId)
                .hmrcHouseholdIdentifier(identityAndEligibilityResponse.getHmrcHouseholdIdentifier()).build();
    }

    private PaymentCycleVoucherEntitlement determinePaymentCycleVoucherEntitlementFromStatus(PaymentCycleVoucherEntitlement entitlement,
                                                                                             EligibilityStatus eligibilityStatus) {
        return (eligibilityStatus == EligibilityStatus.ELIGIBLE) ? entitlement : null;
    }

    private EligibilityStatus determineEligibilityStatus(CombinedIdentityAndEligibilityResponse response,
                                                         PaymentCycleVoucherEntitlement voucherEntitlement,
                                                         boolean duplicateHouseholdIdentifierFound) {
        if (response.isEligible() && voucherEntitlement.getTotalVoucherEntitlement() == 0) {
            return EligibilityStatus.INELIGIBLE;
        } else if (duplicateHouseholdIdentifierFound) {
            return EligibilityStatus.DUPLICATE;
        }
        return fromEligibilityOutcome(response.getEligibilityStatus());
    }

}
