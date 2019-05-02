package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Calculates the entitlement for a claimant over a given payment cycle. Entitlement is calculated for a number of
 * calculation periods and the sum of those entitlements is returned. E.g. a cycle period of 28 days with 4 calculation
 * periods would result in entitlement being calculated four times, each one week apart.
 */
@Component
public class CycleEntitlementCalculator {

    private final Integer entitlementCalculationDuration;
    private final Integer numberOfCalculationPeriods;
    private final EntitlementCalculator entitlementCalculator;
    private final Integer weeksBeforeDueDate;
    private final Integer weeksAfterDueDate;

    /**
     * Constructor for {@link CycleEntitlementCalculator}.
     * @param paymentCycleDurationInDays duration of payment cycle
     * @param numberOfCalculationPeriods number of calculation periods in cycle
     * @param weeksBeforeDueDate number of weeks before expected due date that matches a new child to that expected due date
     * @param weeksAfterDueDate number of weeks after expected due date that matches a new child to that expected due date
     * @param entitlementCalculator calculates entitlement for a single calculation period
     */
    public CycleEntitlementCalculator(@Value("${payment-cycle.cycle-duration-in-days}") Integer paymentCycleDurationInDays,
                                      @Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods,
                                      @Value("${payment-cycle.child-matched-to-pregnancy-period.weeks-before-due-date}") Integer weeksBeforeDueDate,
                                      @Value("${payment-cycle.child-matched-to-pregnancy-period.weeks-after-due-date}") Integer weeksAfterDueDate,
                                      EntitlementCalculator entitlementCalculator) {
        validateArguments(paymentCycleDurationInDays, numberOfCalculationPeriods);
        this.weeksBeforeDueDate = weeksBeforeDueDate;
        this.weeksAfterDueDate = weeksAfterDueDate;
        this.entitlementCalculationDuration = paymentCycleDurationInDays / numberOfCalculationPeriods;
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
        this.entitlementCalculator = entitlementCalculator;
    }

    private void validateArguments(Integer paymentCycleDurationInDays, Integer numberOfCalculationPeriods) {
        if (paymentCycleDurationInDays == 0) {
            throw new IllegalArgumentException("Payment cycle duration can not be zero");
        }
        if (numberOfCalculationPeriods == 0) {
            throw new IllegalArgumentException("Number of calculation periods can not be zero");
        }
        if (paymentCycleDurationInDays % numberOfCalculationPeriods != 0) {
            throw new IllegalArgumentException("Payment cycle duration of " + paymentCycleDurationInDays
                    + " days is not divisible by number of calculation periods " + numberOfCalculationPeriods);
        }
    }

    /**
     * Calculates the total voucher entitlement for a payment cycle when there is no previous voucher entitlement.
     * @param expectedDueDate expected due date
     * @param dateOfBirthOfChildren the date of birth of the claimant's children
     * @return the payment cycle voucher entitlement calculated for the claimant
     */
    public PaymentCycleVoucherEntitlement calculateEntitlement(Optional<LocalDate> expectedDueDate, List<LocalDate> dateOfBirthOfChildren) {
        List<LocalDate> entitlementDates = getEntitlementDates();
        List<VoucherEntitlement> voucherEntitlements = entitlementDates.stream()
                .map(date -> entitlementCalculator.calculateVoucherEntitlement(expectedDueDate, dateOfBirthOfChildren, date))
                .collect(Collectors.toList());

        return new PaymentCycleVoucherEntitlement(voucherEntitlements);
    }

    /**
     * Calculates the total voucher entitlement for a payment cycle given the previous entitlement.
     * @param expectedDueDate expected due date
     * @param dateOfBirthOfChildren the date of birth of the claimant's children
     * @param previousVoucherEntitlement voucher entitlement from last payment cycle
     * @return the payment cycle voucher entitlement calculated for the claimant
     */
    public PaymentCycleVoucherEntitlement calculateEntitlement(Optional<LocalDate> expectedDueDate,
                                                               List<LocalDate> dateOfBirthOfChildren,
                                                               PaymentCycleVoucherEntitlement previousVoucherEntitlement) {
            // ignore expected due date as we've determined that the pregnancy has happened
        if (newChildrenMatchedToExpectedDeliveryDate(expectedDueDate, dateOfBirthOfChildren, previousVoucherEntitlement)) {
            // ignore expected due date as we've determined that the pregnancy has finished
            return calculateEntitlement(Optional.empty(), dateOfBirthOfChildren);
        }

        return calculateEntitlement(expectedDueDate, dateOfBirthOfChildren);
    }

    // A child is considered a result of expected due date if their date of birth falls within a range of that date
    private boolean newChildrenMatchedToExpectedDeliveryDate(Optional<LocalDate> expectedDueDate,
                                                             List<LocalDate> dateOfBirthOfChildren,
                                                             PaymentCycleVoucherEntitlement previousVoucherEntitlement) {
        if (noPregnancyVouchers(previousVoucherEntitlement) || notPregnant(expectedDueDate)) {
            return false;
        }

        return dateOfBirthOfChildren.stream()
                .anyMatch(date -> isWithPregnancyMatchPeriod(expectedDueDate.get(), date));
    }

    private boolean notPregnant(Optional<LocalDate> expectedDueDate) {
        return expectedDueDate.isEmpty();
    }

    private boolean noPregnancyVouchers(PaymentCycleVoucherEntitlement vouchersForPregnancy) {
        return vouchersForPregnancy.getVouchersForPregnancy() == 0;
    }

    private boolean isWithPregnancyMatchPeriod(LocalDate expectedDueDate, LocalDate dateOfBirth) {
        LocalDate startDate = expectedDueDate.minusWeeks(weeksBeforeDueDate);
        LocalDate endDate = expectedDueDate.plusWeeks(weeksAfterDueDate);
        return !dateOfBirth.isBefore(startDate) && !dateOfBirth.isAfter(endDate);
    }

    private List<LocalDate> getEntitlementDates() {
        LocalDate today = LocalDate.now();
        List<LocalDate> entitlementDates = new ArrayList<>(numberOfCalculationPeriods);
        for (int i = 0; i < numberOfCalculationPeriods; i++) {
            entitlementDates.add(today.plusDays(i * entitlementCalculationDuration));
        }
        return entitlementDates;
    }
}
