package uk.gov.dhsc.htbhf.claimant.service;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.util.Optional;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.v2.EligibilityOutcomeToEligibilityStatusConverter.fromEligibilityOutcome;

/**
 * Responsible for building {@link uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision}s based
 * on the provided response object from DWP API call.
 */
@Component
public class EligibilityAndEntitlementDecisionFactory {

    /**
     * Builds a decision based on the given response and entitlement. This will also store the existing claim id on the decision
     * which is indicative of an ineligible claim as there is already a matching live claim in the database.
     *
     * <p>We only return and store a voucher entitlement if the Claimant is ELIGIBLE.
     *
     * @param identityAndEligibilityResponse The response from DWP.
     * @param entitlement                    The already calculated entitlement.
     * @param existingClaimId                The matching live claim id from the db.
     * @param hmrcHouseholdIdentifier        The optional HMRC household identifier to set
     * @return The built decision.
     */
    public EligibilityAndEntitlementDecision buildDecision(IdentityAndEligibilityResponse identityAndEligibilityResponse,
                                                           PaymentCycleVoucherEntitlement entitlement,
                                                           UUID existingClaimId,
                                                           Optional<String> hmrcHouseholdIdentifier) {
        EligibilityStatus eligibilityStatus = determineEligibilityStatus(identityAndEligibilityResponse, entitlement);
        PaymentCycleVoucherEntitlement voucherEntitlement = determinePaymentCycleVoucherEntitlementFromStatus(entitlement, eligibilityStatus);
        EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder builder = EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(eligibilityStatus)
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .voucherEntitlement(voucherEntitlement)
                .dateOfBirthOfChildren(identityAndEligibilityResponse.getDobOfChildrenUnder4())
                .dwpHouseholdIdentifier(identityAndEligibilityResponse.getHouseholdIdentifier())
                .existingClaimId(existingClaimId);
        hmrcHouseholdIdentifier.ifPresent(builder::hmrcHouseholdIdentifier);
        return builder.build();
    }

    /**
     * Builds a decision without a matching existing claim.
     *
     * @param identityAndEligibilityResponse The response from DWP.
     * @param entitlement                    The already calculated entitlement.
     * @param hmrcHouseholdIdentifier        The optional HMRC household identifier to set
     * @return The built decision.
     */
    public EligibilityAndEntitlementDecision buildDecision(IdentityAndEligibilityResponse identityAndEligibilityResponse,
                                                           PaymentCycleVoucherEntitlement entitlement,
                                                           Optional<String> hmrcHouseholdIdentifier) {
        return buildDecision(identityAndEligibilityResponse, entitlement, null, hmrcHouseholdIdentifier);
    }

    private PaymentCycleVoucherEntitlement determinePaymentCycleVoucherEntitlementFromStatus(PaymentCycleVoucherEntitlement entitlement,
                                                                                             EligibilityStatus eligibilityStatus) {
        return (eligibilityStatus == EligibilityStatus.ELIGIBLE) ? entitlement : null;
    }

    private EligibilityStatus determineEligibilityStatus(IdentityAndEligibilityResponse response,
                                                         PaymentCycleVoucherEntitlement voucherEntitlement) {
        if (response.isEligible() && voucherEntitlement.getTotalVoucherEntitlement() == 0) {
            return EligibilityStatus.INELIGIBLE;
        }
        return fromEligibilityOutcome(response.getEligibilityStatus());
    }


}
