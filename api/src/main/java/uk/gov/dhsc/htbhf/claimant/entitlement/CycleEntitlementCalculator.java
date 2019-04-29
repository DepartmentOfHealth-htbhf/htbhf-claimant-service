package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class CycleEntitlementCalculator {

    private final Integer entitlementCalculationDuration;
    private final Integer numberOfCalculationPeriods;
    private final EntitlementCalculator entitlementCalculator;

    public CycleEntitlementCalculator(@Value("${payment-cycle.cycle-duration-in-days}") Integer paymentCycleDurationInDays,
                                      @Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods,
                                      EntitlementCalculator entitlementCalculator) {
        // TODO check numbers are divisible
        this.entitlementCalculationDuration = paymentCycleDurationInDays / numberOfCalculationPeriods;
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
        this.entitlementCalculator = entitlementCalculator;
    }

    public int calculateEntitlementInPence(Claimant claimant, List<LocalDate> childrenDatesOfBirth) {
        LocalDate today = LocalDate.now();
        List<LocalDate> entitlementDates = getEntitlementDates(today);

        return entitlementDates.stream()
                .map(date -> entitlementCalculator.calculateVoucherEntitlement(claimant, childrenDatesOfBirth, date))
                .mapToInt(VoucherEntitlement::getTotalVoucherValueInPence)
                .sum();
    }

    private List<LocalDate> getEntitlementDates(LocalDate today) {
        List<LocalDate> entitlementDates = new ArrayList<>(numberOfCalculationPeriods);
        for (int i = 0; i < numberOfCalculationPeriods; i++) {
            entitlementDates.add(today.plusDays(i * entitlementCalculationDuration));
        }
        return entitlementDates;
    }
}
