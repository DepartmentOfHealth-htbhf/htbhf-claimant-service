package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.springframework.beans.factory.annotation.Value;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

public class PaymentCalculator {

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
     * @param entitlement a payment cycle voucher entitlement
     * @param cardBalanceInPence the card balance in pence
     * @return the amount in pence to apply to the next payment request
     */
    public int calculatePaymentAmountCycleInPence(PaymentCycleVoucherEntitlement entitlement, int cardBalanceInPence) {
        int firstWeekEntitlementInPence = entitlement.getFirstVoucherEntitlementForCycle().getTotalVoucherValueInPence();
        int maximumAllowedCardBalanceInPence = firstWeekEntitlementInPence * maximumBalancePeriod;

        if (isCardBalanceTooHigh(cardBalanceInPence, maximumAllowedCardBalanceInPence)) {
            return 0;
        }
        int paymentCycleTotalEntitlementInPence = entitlement.getTotalVoucherValueInPence();
        if (fullPaymentKeepsBalanceUnderThreshold(cardBalanceInPence, maximumAllowedCardBalanceInPence, paymentCycleTotalEntitlementInPence)) {
            return paymentCycleTotalEntitlementInPence;
        }
        return calculatePartialPayment(cardBalanceInPence, maximumAllowedCardBalanceInPence);
    }

    private int calculatePartialPayment(int cardBalanceInPence, int maximumAllowedCardBalanceInPence) {
        return maximumAllowedCardBalanceInPence - cardBalanceInPence;
    }

    private boolean isCardBalanceTooHigh(int cardBalanceInPence, int maximumAllowedCardBalanceInPence) {
        return cardBalanceInPence >= maximumAllowedCardBalanceInPence;
    }

    private boolean fullPaymentKeepsBalanceUnderThreshold(int cardBalanceInPence, int maxAllowedCardBalanceInPence, int paymentCycleTotalEntitlementInPence) {
        return cardBalanceInPence + paymentCycleTotalEntitlementInPence < maxAllowedCardBalanceInPence;
    }
}
