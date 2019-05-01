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

    public CycleEntitlementCalculator(@Value("${payment-cycle.cycle-duration-in-days}") Integer paymentCycleDurationInDays,
                                      @Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods,
                                      @Value("${payment-cycle.child-from-pregnancy.weeks-before-due-date}") Integer weeksBeforeDueDate,
                                      @Value("${payment-cycle.child-from-pregnancy.weeks-after-due-date}") Integer weeksAfterDueDate,
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

    public PaymentCycleVoucherEntitlement calculateEntitlement(Optional<LocalDate> expectedDueDate, List<LocalDate> dateOfBirthOfChildren) {
        List<LocalDate> entitlementDates = getEntitlementDates();
        List<VoucherEntitlement> voucherEntitlements = entitlementDates.stream()
                .map(date -> entitlementCalculator.calculateVoucherEntitlement(expectedDueDate, dateOfBirthOfChildren, date))
                .collect(Collectors.toList());

        return new PaymentCycleVoucherEntitlement(voucherEntitlements);
    }

    public PaymentCycleVoucherEntitlement calculateEntitlement(Optional<LocalDate> expectedDueDate,
                                                               List<LocalDate> dateOfBirthOfChildren,
                                                               PaymentCycleVoucherEntitlement previousVoucherEntitlement) {
        if (previousVoucherEntitlement.getVouchersForPregnancy() == 0 || expectedDueDate.isEmpty()) {
            return calculateEntitlement(expectedDueDate, dateOfBirthOfChildren);
        }

        if (newChildrenFromPregnancyExist(expectedDueDate.get(), dateOfBirthOfChildren)) {
            // ignore expected due date as we've determined that the pregnancy has happened
            return calculateEntitlement(Optional.empty(), dateOfBirthOfChildren);
        }

        return calculateEntitlement(expectedDueDate, dateOfBirthOfChildren);
    }

    // A child is considered a result of pregnancy if their date of birth falls within a date range relative to the expected due date
    private boolean newChildrenFromPregnancyExist(LocalDate expectedDueDate, List<LocalDate> dateOfBirthOfChildren) {
        LocalDate startDate = expectedDueDate.minusWeeks(weeksBeforeDueDate);
        LocalDate endDate = expectedDueDate.plusWeeks(weeksAfterDueDate);
        return dateOfBirthOfChildren.stream()
                .anyMatch(date -> date.isAfter(startDate) && date.isBefore(endDate));
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
