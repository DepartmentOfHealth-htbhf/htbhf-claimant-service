package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;

@Component
@AllArgsConstructor
public class CycleEntitlementCalculator {

    private final EntitlementCalculator entitlementCalculator;

    public int calculateEntitlementInPence(Claimant claimant, List<LocalDate> childrenDatesOfBirth) {

        List<LocalDate> entitlementDates = asList(
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(14),
                LocalDate.now().plusDays(21));

        return entitlementDates.stream()
                .map(date -> entitlementCalculator.calculateVoucherEntitlement(claimant, childrenDatesOfBirth, date))
                .mapToInt(VoucherEntitlement::getTotalVoucherValueInPence)
                .sum();
    }
}
