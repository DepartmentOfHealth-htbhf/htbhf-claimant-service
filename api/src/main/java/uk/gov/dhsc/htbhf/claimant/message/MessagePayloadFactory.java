package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.ReportClaimMessagePayload;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Factory object for building message payloads for emails.
 */
public class MessagePayloadFactory {

    public static NewCardRequestMessagePayload buildNewCardMessagePayload(Claim claim,
                                                                          PaymentCycleVoucherEntitlement voucherEntitlement,
                                                                          List<LocalDate> datesOfBirthOfChildren) {
        return NewCardRequestMessagePayload.builder()
                .claimId(claim.getId())
                .voucherEntitlement(voucherEntitlement)
                .datesOfBirthOfChildren(datesOfBirthOfChildren)
                .build();
    }

    public static MakePaymentMessagePayload buildMakePaymentMessagePayload(PaymentCycle paymentCycle) {
        return MakePaymentMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .paymentCycleId(paymentCycle.getId())
                .cardAccountId(paymentCycle.getClaim().getCardAccountId())
                .build();
    }

    public static ReportClaimMessagePayload buildReportClaimMessagePayload(Claim claim, List<LocalDate> dateOfBirthOfChildren, ClaimAction claimAction) {
        return ReportClaimMessagePayload.builder()
                .claimId(claim.getId())
                .datesOfBirthOfChildren(dateOfBirthOfChildren)
                .claimAction(claimAction)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
