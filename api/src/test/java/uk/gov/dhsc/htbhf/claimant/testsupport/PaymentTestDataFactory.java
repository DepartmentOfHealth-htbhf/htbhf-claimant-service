package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;

import java.time.LocalDateTime;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

public class PaymentTestDataFactory {

    private static final Claim CLAIM = aValidClaim();
    private static final int PAYMENT_AMOUNT_IN_PENCE = 310;
    private static final LocalDateTime PAYMENT_TIMESTAMP = LocalDateTime.now();
    private static final String PAYMENT_REFERENCE = "123456789";

    public static Payment aValidPayment() {
        return aValidPaymentBuilder().build();
    }

    public static Payment aPaymentWithClaim(Claim claim) {
        return aValidPaymentBuilder().claim(claim).build();
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

    public static Payment aPaymentWithPaymentStatus(PaymentStatus paymentStatus) {
        return aValidPaymentBuilder().paymentStatus(paymentStatus).build();
    }

    private static Payment.PaymentBuilder aValidPaymentBuilder() {
        return Payment.builder()
                .claim(CLAIM)
                .cardAccountId(CARD_ACCOUNT_ID)
                .paymentAmountInPence(PAYMENT_AMOUNT_IN_PENCE)
                .paymentTimestamp(PAYMENT_TIMESTAMP)
                .paymentReference(PAYMENT_REFERENCE)
                .paymentStatus(PaymentStatus.SUCCESS);
    }
}
