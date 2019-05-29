package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;

import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.BALANCE_TOO_HIGH_FOR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.FULL_PAYMENT_MADE;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.PARTIAL_PAYMENT_MADE;

public class PaymentCalculationTestDataFactory {

    public static PaymentCalculation aNoPaymentCalculation() {
        return buildPaymentCalculation(0, BALANCE_TOO_HIGH_FOR_PAYMENT);
    }

    public static PaymentCalculation aPartialPaymentCalculation() {
        return buildPaymentCalculation(4960, PARTIAL_PAYMENT_MADE);
    }

    public static PaymentCalculation aFullPaymentCalculation() {
        return buildPaymentCalculation(1240, FULL_PAYMENT_MADE);
    }

    private static PaymentCalculation buildPaymentCalculation(int paymentAmount, PaymentCycleStatus paymentCycleStatus) {
        return PaymentCalculation.builder()
                .paymentAmount(paymentAmount)
                .paymentCycleStatus(paymentCycleStatus)
                .build();
    }
}
