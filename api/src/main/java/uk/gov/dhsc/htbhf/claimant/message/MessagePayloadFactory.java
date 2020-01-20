package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;
import uk.gov.dhsc.htbhf.claimant.message.payload.ReportClaimMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.RequestNewCardMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.RequestPaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Factory object for building message payloads for emails.
 */
public class MessagePayloadFactory {

    public static RequestNewCardMessagePayload buildNewCardMessagePayload(Claim claim,
                                                                          EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision) {
        return RequestNewCardMessagePayload.builder()
                .claimId(claim.getId())
                .eligibilityAndEntitlementDecision(eligibilityAndEntitlementDecision)
                .build();
    }

    public static RequestPaymentMessagePayload buildRequestPaymentMessagePayload(PaymentCycle paymentCycle, PaymentType paymentType) {
        return RequestPaymentMessagePayload.builder()
                .paymentCycleId(paymentCycle.getId())
                .claimId(paymentCycle.getClaim().getId())
                .paymentType(paymentType)
                .build();
    }

    public static ReportClaimMessagePayload buildReportClaimMessagePayload(Claim claim,
                                                                           CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse,
                                                                           ClaimAction claimAction,
                                                                           List<UpdatableClaimantField> updatedClaimantFields) {
        return ReportClaimMessagePayload.builder()
                .claimId(claim.getId())
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .claimAction(claimAction)
                .timestamp(LocalDateTime.now())
                .updatedClaimantFields(updatedClaimantFields)
                .build();
    }
}
