package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.*;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.message.MoneyUtils.convertPenceToPounds;

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

    public static ReportClaimMessagePayload buildReportClaimMessagePayload(Claim claim) {
        return ReportClaimMessagePayload.builder()
                .claimId(claim.getId())
                .build();
    }

    public static String formatPaymentAmountSummary(String summaryTemplate, int numberOfVouchers, int voucherAmountInPence) {
        if (numberOfVouchers == 0) {
            return "";
        }
        int totalAmount = numberOfVouchers * voucherAmountInPence;
        return String.format(summaryTemplate, convertPenceToPounds(totalAmount));
    }

}
