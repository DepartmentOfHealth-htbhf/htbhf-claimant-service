package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.BALANCE_TOO_HIGH_FOR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.FULL_PAYMENT_MADE;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.PARTIAL_PAYMENT_MADE;

@Component
public class PaymentCalculator {

    private static final PaymentCalculation NO_PAYMENT_CALCULATION = PaymentCalculation.builder()
            .paymentAmount(0)
            .paymentCycleStatus(BALANCE_TOO_HIGH_FOR_PAYMENT)
            .build();

    private final int maximumBalancePeriod;

    public PaymentCalculator(@Value("${payment-cycle.maximum-balance-period}") int maximumBalancePeriod) {
        this.maximumBalancePeriod = maximumBalancePeriod;
    }

    /**
     * Calculates the amount to apply to a new payment request.
     * Returns the full entitlement amount when the full entitlement amount plus the card balance is less than or equal to the maximum card balance.
     * Returns a partial amount when the full entitlement amount plus the card balance is greater than or equal to the maximum card balance.
     * Returns zero when the card balance is greater than or equal to the maximum card balance.
     *
     * @param entitlement        a payment cycle voucher entitlement
     * @param cardBalanceInPence the card balance in pence
     * @return the payment calculation which comprises the amount in pence to apply to the next payment request and the status of the PaymentCycle
     */
    public PaymentCalculation calculatePaymentCycleAmountInPence(PaymentCycleVoucherEntitlement entitlement, int cardBalanceInPence) {
        int firstWeekEntitlementInPence = entitlement.getFirstVoucherEntitlementForCycle().getTotalVoucherValueInPence();
        int maximumAllowedCardBalanceInPence = firstWeekEntitlementInPence * maximumBalancePeriod;

        if (isCardBalanceTooHigh(cardBalanceInPence, maximumAllowedCardBalanceInPence)) {
            return NO_PAYMENT_CALCULATION;
        }
        int paymentCycleTotalEntitlementInPence = entitlement.getTotalVoucherValueInPence();
        if (fullPaymentKeepsBalanceLessThanOrEqualToThreshold(cardBalanceInPence, maximumAllowedCardBalanceInPence, paymentCycleTotalEntitlementInPence)) {
            return PaymentCalculation.builder()
                    .paymentAmount(paymentCycleTotalEntitlementInPence)
                    .paymentCycleStatus(FULL_PAYMENT_MADE)
                    .build();
        }
        return PaymentCalculation.builder()
                .paymentAmount(maximumAllowedCardBalanceInPence - cardBalanceInPence)
                .paymentCycleStatus(PARTIAL_PAYMENT_MADE)
                .build();
    }

    private boolean isCardBalanceTooHigh(int cardBalanceInPence, int maximumAllowedCardBalanceInPence) {
        return cardBalanceInPence >= maximumAllowedCardBalanceInPence;
    }

    private boolean fullPaymentKeepsBalanceLessThanOrEqualToThreshold(int cardBalanceInPence,
                                                                      int maxAllowedCardBalanceInPence,
                                                                      int paymentCycleTotalEntitlementInPence) {
        return cardBalanceInPence + paymentCycleTotalEntitlementInPence <= maxAllowedCardBalanceInPence;
    }
}
