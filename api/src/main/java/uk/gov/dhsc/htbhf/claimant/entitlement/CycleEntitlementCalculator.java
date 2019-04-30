package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    public CycleEntitlementCalculator(@Value("${payment-cycle.cycle-duration-in-days}") Integer paymentCycleDurationInDays,
                                      @Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods,
                                      EntitlementCalculator entitlementCalculator) {
        validateArguments(paymentCycleDurationInDays, numberOfCalculationPeriods);
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

    public PaymentCycleVoucherEntitlement calculateCycleVoucherEntitlement(Claimant claimant, List<LocalDate> dateOfBirthOfChildren) {
        List<LocalDate> entitlementDates = getEntitlementDates();
        List<VoucherEntitlement> voucherEntitlements = entitlementDates.stream()
                .map(date -> entitlementCalculator.calculateVoucherEntitlement(claimant, dateOfBirthOfChildren, date))
                .collect(Collectors.toList());

        return new PaymentCycleVoucherEntitlement(voucherEntitlements);
    }

    public PaymentCycleVoucherEntitlement calculateCycleVoucherEntitlement(Claimant claimant,
                                                                           List<LocalDate> dateOfBirthOfChildren,
                                                                           PaymentCycleVoucherEntitlement previousVoucherEntitlement) {
        if (previousVoucherEntitlement.getVouchersForPregnancy() == 0) {
            return calculateCycleVoucherEntitlement(claimant, dateOfBirthOfChildren);
        }

        if (noNewChildrenFromPregnancyExist(claimant, dateOfBirthOfChildren)) {
            return calculateCycleVoucherEntitlement(claimant, dateOfBirthOfChildren);
        }
    }

    private boolean noNewChildrenFromPregnancyExist(Claimant claimant, List<LocalDate> dateOfBirthOfChildren) {
        // TODO: use config for date range and update variable names
        LocalDate beginning = claimant.getExpectedDeliveryDate().minusWeeks(16);
        LocalDate end = claimant.getExpectedDeliveryDate().plusWeeks(8);
        return dateOfBirthOfChildren.stream()
                .noneMatch(date -> date.isAfter(beginning) && date.isBefore(end));
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
