package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.ReportClaimMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.RequestNewCardMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.time.LocalDate;
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

    public static MakePaymentMessagePayload buildMakePaymentMessagePayload(PaymentCycle paymentCycle) {
        return MakePaymentMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .paymentCycleId(paymentCycle.getId())
                .cardAccountId(paymentCycle.getClaim().getCardAccountId())
                .paymentRestarted(false)
                .build();
    }

    public static MakePaymentMessagePayload buildMakePaymentMessagePayloadForRestartedPayment(PaymentCycle paymentCycle) {
        return MakePaymentMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .paymentCycleId(paymentCycle.getId())
                .cardAccountId(paymentCycle.getClaim().getCardAccountId())
                .paymentRestarted(true)
                .build();
    }

    public static ReportClaimMessagePayload buildReportClaimMessagePayload(Claim claim,
                                                                           List<LocalDate> dateOfBirthOfChildren,
                                                                           ClaimAction claimAction,
                                                                           List<UpdatableClaimantField> updatedClaimantFields) {
        return ReportClaimMessagePayload.builder()
                .claimId(claim.getId())
                .datesOfBirthOfChildren(dateOfBirthOfChildren)
                .claimAction(claimAction)
                .timestamp(LocalDateTime.now())
                .updatedClaimantFields(updatedClaimantFields)
                .build();
    }
}
