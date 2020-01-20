package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;

import java.time.LocalDateTime;

import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.BALANCE_TOO_HIGH_FOR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.FULL_PAYMENT_MADE;

/**
 * The result of a payment calculation from {@link PaymentCalculator}.
 */
@Value
@Builder
public class PaymentCalculation {

    private PaymentCycleStatus paymentCycleStatus;
    private int paymentAmount;
    private int availableBalanceInPence;
    private LocalDateTime balanceTimestamp;

    public static PaymentCalculation aFullPaymentCalculationWithZeroBalance(int paymentAmount) {
        return PaymentCalculation.builder()
                .paymentAmount(paymentAmount)
                .paymentCycleStatus(FULL_PAYMENT_MADE)
                .balanceTimestamp(LocalDateTime.now())
                .availableBalanceInPence(0)
                .build();
    }

    public static PaymentCalculation aBalanceTooHighPaymentCalculation(int availableBalanceInPence) {
        return PaymentCalculation.builder()
                .availableBalanceInPence(availableBalanceInPence)
                .balanceTimestamp(LocalDateTime.now())
                .paymentCycleStatus(BALANCE_TOO_HIGH_FOR_PAYMENT)
                .paymentAmount(0)
                .build();
    }

}
