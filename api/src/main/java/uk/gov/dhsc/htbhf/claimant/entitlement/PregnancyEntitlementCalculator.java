package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.dwp.model.QualifyingReason;

import java.time.LocalDate;

import static uk.gov.dhsc.htbhf.dwp.model.QualifyingReason.UNDER_18;

/**
 * Responsible for deciding whether a claimant is entitled to a voucher for pregnancy,
 * by comparing the due date to a given entitlement date.
 * There is a grace period after the due date before the claimant stops being eligible for a voucher.
 */
@Component
public class PregnancyEntitlementCalculator {

    private final int pregnancyGracePeriodInWeeks;
    private final int paymentCycleDurationInDays;
    private final int under18PregnancyGracePeriodInWeeks;

    public PregnancyEntitlementCalculator(@Value("${entitlement.pregnancy-grace-period-in-weeks}") int pregnancyGracePeriodInWeeks,
                                          @Value("${payment-cycle.cycle-duration-in-days}") int paymentCycleDurationInDays,
                                          @Value("${entitlement.under-eighteen-pregnancy-grace-period-in-weeks}") int under18PregnancyGracePeriodInWeeks) {
        this.pregnancyGracePeriodInWeeks = pregnancyGracePeriodInWeeks;
        this.paymentCycleDurationInDays = paymentCycleDurationInDays;
        this.under18PregnancyGracePeriodInWeeks = under18PregnancyGracePeriodInWeeks;
    }

    public boolean isEntitledToVoucher(LocalDate dueDate, LocalDate entitlementDate, QualifyingReason qualifyingReason) {
        if (entitlementDate == null) {
            throw new IllegalArgumentException("entitlementDate must not be null");
        }
        if (dueDate == null) {
            return false;
        }

        LocalDate endOfGracePeriod;
        if (qualifyingReason == UNDER_18) {
            endOfGracePeriod = dueDate.plusWeeks(under18PregnancyGracePeriodInWeeks);
        } else {
            endOfGracePeriod = dueDate.plusWeeks(pregnancyGracePeriodInWeeks);
        }

        return !endOfGracePeriod.isBefore(entitlementDate);
    }

    /**
     * Determines if the current cycle is the claimant's second to last cycle with pregnancy vouchers. i.e if the current and next payment cycle are
     * entitled to pregnancy vouchers, and the one after that is not entitled to vouchers.
     * <p>
     * Example illustrated below:
     * |...|...|...|...| four payment cycles (16 weeks)
     *   ^ due date
     *         |...| second to last cycle with pregnancy vouchers
     *               ^ end of grace period for pregnancy vouchers
     *             |...| last cycle with pregnancy vouchers
     * </p>
     * @param currentPaymentCycle the current payment cycle
     * @return true if the current and next payment cycle are entitled to pregnancy vouchers, and the one after that is not entitled to vouchers.
     */
    public boolean currentCycleIsSecondToLastCycleWithPregnancyVouchers(PaymentCycle currentPaymentCycle) {
        LocalDate expectedDeliveryDate = currentPaymentCycle.getExpectedDeliveryDate();
        LocalDate cycleStartDate = currentPaymentCycle.getCycleStartDate();
        QualifyingReason qualifyingReason = getQualifyingReasonFromCombinedIdentityAndEligibilityResponse(currentPaymentCycle);
        boolean entitledToVouchersInCurrentCycle = isEntitledToVoucher(expectedDeliveryDate, cycleStartDate, qualifyingReason);
        LocalDate nextCycleStartDate = cycleStartDate.plusDays(paymentCycleDurationInDays);
        boolean entitledToVouchersInNextCycle = isEntitledToVoucher(expectedDeliveryDate, nextCycleStartDate, qualifyingReason);
        LocalDate cycleAfterNextStartDate = nextCycleStartDate.plusDays(paymentCycleDurationInDays);
        boolean entitledToVouchersInCycleAfterNext = isEntitledToVoucher(expectedDeliveryDate, cycleAfterNextStartDate, qualifyingReason);

        return entitledToVouchersInCurrentCycle && entitledToVouchersInNextCycle && !entitledToVouchersInCycleAfterNext;
    }

    private QualifyingReason getQualifyingReasonFromCombinedIdentityAndEligibilityResponse(PaymentCycle paymentCycle) {
        return paymentCycle.getClaim().getCurrentIdentityAndEligibilityResponse().getQualifyingReason();
    }

    /**
     * Determines if a claimant is pregnant in the payment cycle based on their due date and the pregnancy grace period.
     * Returns true if the claimant's due date is before or equal to the payment cycle start date plus the grace period duration.
     * @param paymentCycle the payment cycle to check when the claimant is pregnant
     *
     * @return true if the claimant's due date is before or equal to the payment cycle start date plus the grace period duration.
     */
    public boolean claimantIsPregnantInCycle(PaymentCycle paymentCycle) {
        QualifyingReason qualifyingReason = getQualifyingReasonFromCombinedIdentityAndEligibilityResponse(paymentCycle);
        return isEntitledToVoucher(paymentCycle.getExpectedDeliveryDate(), paymentCycle.getCycleStartDate(), qualifyingReason);
    }
    
    /**
     * Determines if a claimant is still pregnant on the day after the payment cycle ends, based on their due date and the pregnancy grace period.
     * Returns true if the claimant's due date is before or equal to the day after payment cycle end date plus the grace period duration.
     * @param paymentCycle the payment cycle to check when the claimant is pregnant
     *
     * @return true if the claimant is entitled to a voucher on the day after the given cycle ends.
     */
    public boolean claimantIsPregnantAfterCycle(PaymentCycle paymentCycle) {
        LocalDate expectedDeliveryDate = paymentCycle.getExpectedDeliveryDate();
        LocalDate nextCycleStartDate = paymentCycle.getCycleEndDate().plusDays(1);
        QualifyingReason qualifyingReason = getQualifyingReasonFromCombinedIdentityAndEligibilityResponse(paymentCycle);
        return isEntitledToVoucher(expectedDeliveryDate, nextCycleStartDate, qualifyingReason);
    }

}
