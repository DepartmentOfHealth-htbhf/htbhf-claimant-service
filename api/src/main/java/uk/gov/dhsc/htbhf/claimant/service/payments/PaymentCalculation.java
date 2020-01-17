package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;

import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.BALANCE_TOO_HIGH_FOR_PAYMENT;

/**
 * The result of a payment calculation from {@link PaymentCalculator}.
 */
@Value
@Builder
public class PaymentCalculation {

    private PaymentCycleStatus paymentCycleStatus;

    private int paymentAmount;

    private int availableBalanceInPence;

    public static PaymentCalculation aFullPaymentCalculation(int paymentAmount) {
        return PaymentCalculation.builder()
                .paymentAmount(paymentAmount)
                .paymentCycleStatus(PaymentCycleStatus.FULL_PAYMENT_MADE)
                .build();
    }

    public static PaymentCalculation aBalanceTooHighPaymentCalculation(int availableBalanceInPence) {
        return PaymentCalculation.builder()
                .availableBalanceInPence(availableBalanceInPence)
                .paymentCycleStatus(BALANCE_TOO_HIGH_FOR_PAYMENT)
                .paymentAmount(0)
                .build();
    }
}
