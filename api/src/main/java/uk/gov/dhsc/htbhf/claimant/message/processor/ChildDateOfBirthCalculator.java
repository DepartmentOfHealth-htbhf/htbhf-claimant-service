package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.dhsc.htbhf.claimant.entitlement.CycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;

/**
 * Contains utility methods for helping to figure out information pertaining to the dates of birth
 * for the Claimant.
 */
@Component
public class ChildDateOfBirthCalculator {

    private final CycleEntitlementCalculator cycleEntitlementCalculator;

    public ChildDateOfBirthCalculator(CycleEntitlementCalculator cycleEntitlementCalculator) {
        this.cycleEntitlementCalculator = cycleEntitlementCalculator;
    }

    /**
     * Calculates how many of the children's dates of birth listed on the given PaymentCycle have birthdays
     * which would affect the next payment after this cycle. The dates that would affect the next payment
     * are between the last entitlement date of the current cycle and the final entitlement date of the next
     * cycle. Those with birthdays on the start boundary date (the date of the final entitlement date of
     * the current cycle) are not included, but those on the end boundary date (the final entitlement date
     * of the next cycle) are included.
     *
     * @param paymentCycle The current PaymentCycle including the relevant dates of birth of the children.
     * @return The number of children who will soon turn 4 that affect the next Payment.
     */
    public int getNumberOfChildrenTurningFourAffectingNextPayment(PaymentCycle paymentCycle) {
        return getNumberOfChildrenWithBirthdayAffectingNextPayment(paymentCycle, 4);
    }

    /**
     * As per the method above but for children turning 1.
     *
     * @param paymentCycle The current PaymentCycle including the relevant dates of birth of the children.
     * @return The number of children who will soon turn 1 that affect the next Payment.
     */
    public int getNumberOfChildrenTurningOneAffectingNextPayment(PaymentCycle paymentCycle) {
        return getNumberOfChildrenWithBirthdayAffectingNextPayment(paymentCycle, 1);
    }

    private int getNumberOfChildrenWithBirthdayAffectingNextPayment(PaymentCycle paymentCycle, int age) {
        if (CollectionUtils.isEmpty(paymentCycle.getChildrenDob())) {
            return 0;
        }
        LocalDate currentCycleStartDate = paymentCycle.getCycleStartDate();
        LocalDate nextCycleStartDate = paymentCycle.getCycleEndDate().plusDays(1);
        LocalDate lastEntitlementDateInCurrentCycle = getLatestEntitlementDateFromCycleStartDate(currentCycleStartDate);
        LocalDate lastEntitlementDateInNextCycle = getLatestEntitlementDateFromCycleStartDate(nextCycleStartDate);
        return Math.toIntExact(paymentCycle.getChildrenDob().stream()
                .filter(childDob -> isWithinPeriodExcludingStartDateIncludingEndDate(
                        lastEntitlementDateInCurrentCycle,
                        lastEntitlementDateInNextCycle,
                        age,
                        childDob))
                .count());
    }

    private LocalDate getLatestEntitlementDateFromCycleStartDate(LocalDate cycleStartDate) {
        return cycleEntitlementCalculator.getVoucherEntitlementDatesFromStartDate(cycleStartDate)
                .stream().max(LocalDate::compareTo).get();
    }

    private boolean isWithinPeriodExcludingStartDateIncludingEndDate(LocalDate lastEntitlementDateInCurrentCycle,
                                                                     LocalDate lastEntitlementDateInNextCycle,
                                                                     int age,
                                                                     LocalDate childDob) {
        return childDob.isAfter(lastEntitlementDateInCurrentCycle.minusYears(age))
                && childDob.isBefore(lastEntitlementDateInNextCycle.minusYears(age).plusDays(1));
    }

}
