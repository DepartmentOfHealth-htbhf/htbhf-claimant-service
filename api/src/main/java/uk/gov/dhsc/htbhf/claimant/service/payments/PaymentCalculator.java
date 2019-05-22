package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.springframework.beans.factory.annotation.Value;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

public class PaymentCalculator {

    private final int numberOfCalculationPeriods;
    private final int calculationPeriodThreshold;

    public PaymentCalculator(@Value("${payment-cycle.number-of-calculation-periods}") int numberOfCalculationPeriods) {
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
        this.calculationPeriodThreshold = numberOfCalculationPeriods * 2;
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
        int totalVoucherValueInPence = entitlement.getFirstVoucherEntitlementForCycle().getTotalVoucherValueInPence();
        int maxAllowedCardBalanceInPence = totalVoucherValueInPence * calculationPeriodThreshold;

        if (cardBalanceInPence >= maxAllowedCardBalanceInPence) {
            return 0;
        }

        int partialPaymentThresholdInPence = totalVoucherValueInPence * numberOfCalculationPeriods;

        if (cardBalanceInPence >= partialPaymentThresholdInPence) {
            return maxAllowedCardBalanceInPence - cardBalanceInPence;
        }

        return entitlement.getTotalVoucherValueInPence();
    }
}
