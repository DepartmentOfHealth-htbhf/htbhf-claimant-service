package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;

import static uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary.NO_CHILDREN;

/**
 * Contains utility methods for helping to figure out information pertaining to the dates of birth
 * for the Claimant.
 */
@Component
public class ChildDateOfBirthCalculator {

    private final PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;

    public ChildDateOfBirthCalculator(PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator) {
        this.paymentCycleEntitlementCalculator = paymentCycleEntitlementCalculator;
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
     * @return The number of children who will soon turn 1 or 4 that affect the next Payment.
     */
    public NextPaymentCycleSummary getChildrenDateOfBirthSummaryAffectingNextPayment(PaymentCycle paymentCycle) {
        if (CollectionUtils.isEmpty(paymentCycle.getChildrenDob())) {
            return NO_CHILDREN;
        }
        LocalDate currentCycleStartDate = paymentCycle.getCycleStartDate();
        LocalDate nextCycleStartDate = paymentCycle.getCycleEndDate().plusDays(1);
        LocalDate lastEntitlementDateInCurrentCycle = getLatestEntitlementDateFromCycleStartDate(currentCycleStartDate);
        LocalDate lastEntitlementDateInNextCycle = getLatestEntitlementDateFromCycleStartDate(nextCycleStartDate);
        int childrenAgedOneAffectingNextPayment = countChildrenOfAge(paymentCycle, lastEntitlementDateInCurrentCycle, lastEntitlementDateInNextCycle, 1);
        int childrenAgedFourAffectingNextPayment = countChildrenOfAge(paymentCycle, lastEntitlementDateInCurrentCycle, lastEntitlementDateInNextCycle, 4);
        return NextPaymentCycleSummary.builder()
                .numberOfChildrenTurningOne(childrenAgedOneAffectingNextPayment)
                .numberOfChildrenTurningFour(childrenAgedFourAffectingNextPayment)
                .build();
    }

    private int countChildrenOfAge(PaymentCycle paymentCycle, LocalDate lastEntitlementDateInCurrentCycle,
                                   LocalDate lastEntitlementDateInNextCycle, int age) {
        return Math.toIntExact(paymentCycle.getChildrenDob().stream()
                .filter(childDob -> isWithinPeriodExcludingStartDateIncludingEndDate(
                        lastEntitlementDateInCurrentCycle,
                        lastEntitlementDateInNextCycle,
                        age,
                        childDob))
                .count());
    }

    private LocalDate getLatestEntitlementDateFromCycleStartDate(LocalDate cycleStartDate) {
        return paymentCycleEntitlementCalculator.getVoucherEntitlementDatesFromStartDate(cycleStartDate)
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
