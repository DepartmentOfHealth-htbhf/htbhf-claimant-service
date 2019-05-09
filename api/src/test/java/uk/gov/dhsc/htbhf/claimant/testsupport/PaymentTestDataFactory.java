package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

public class PaymentTestDataFactory {

    private static final UUID CLAIM_ID = aValidClaim().getId();
    private static final String CARD_ACCOUNT_ID = "123456789";
    private static final int PAYMENT_AMOUNT_IN_PENCE = 310;
    private static final LocalDateTime PAYMENT_TIMESTAMP = LocalDateTime.now();
    private static final String PAYMENT_REFERENCE = "123456789";

    public static Payment aValidPayment() {
        return aValidPaymentBuilder().build();
    }

    public static Payment aPaymentWithClaimId(UUID claimId) {
        return aValidPaymentBuilder().claimId(claimId).build();
    }

    public static Payment aPaymentWithCardAccountId(String cardAccountId) {
        return aValidPaymentBuilder().cardAccountId(cardAccountId).build();
    }

    public static Payment aPaymentWithPaymentAmountInPence(Integer paymentAmountInPence) {
        return aValidPaymentBuilder().paymentAmountInPence(paymentAmountInPence).build();
    }

    public static Payment aPaymentWithPaymentTimestamp(LocalDateTime paymentTimestamp) {
        return aValidPaymentBuilder().paymentTimestamp(paymentTimestamp).build();
    }

    public static Payment aPaymentWithPaymentReference(String paymentReference) {
        return aValidPaymentBuilder().paymentReference(paymentReference).build();
    }

    public static Payment aPaymentWithPaymentStatus(PaymentStatus paymentStatus) {
        return aValidPaymentBuilder().paymentStatus(paymentStatus).build();
    }

    private static Payment.PaymentBuilder aValidPaymentBuilder() {
        return Payment.builder()
                .claimId(CLAIM_ID)
                .cardAccountId(CARD_ACCOUNT_ID)
                .paymentAmountInPence(PAYMENT_AMOUNT_IN_PENCE)
                .paymentTimestamp(PAYMENT_TIMESTAMP)
                .paymentReference(PAYMENT_REFERENCE)
                .paymentStatus(PaymentStatus.SUCCESS);
    }
}
