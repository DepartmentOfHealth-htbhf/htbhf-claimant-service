package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.min;

/**
 * Calculates the number of back dated vouchers a claimant is entitlement to.
 * The new children that have been matched to the expected due date and the expected due date are used to
 * calculate the difference between the number of vouchers that were received for pregnancy and
 * the number of vouchers that should have been received for the new children. If the difference in vouchers
 * is less than zero, zero is returned.
 */
@Service
public class BackDatedCycleEntitlementCalculator {

    private final Integer entitlementCalculationDurationInDays;
    private final EntitlementCalculator entitlementCalculator;

    /**
     * Constructor for {@link BackDatedCycleEntitlementCalculator}.
     *
     * @param paymentCycleConfig    configuration for the payment cycle
     * @param entitlementCalculator calculates entitlement for a single calculation period
     */
    public BackDatedCycleEntitlementCalculator(PaymentCycleConfig paymentCycleConfig,
                                               EntitlementCalculator entitlementCalculator) {
        this.entitlementCalculationDurationInDays = paymentCycleConfig.getEntitlementCalculationDurationInDays();
        this.entitlementCalculator = entitlementCalculator;
    }

    public Integer calculateBackDatedVouchers(Optional<LocalDate> expectedDueDate, List<LocalDate> newChildrenDateOfBirths) {
        List<LocalDate> backDatedEntitlementDates = getBackDatedEntitlementDates(newChildrenDateOfBirths);
        Integer vouchersForChildren = calculateNumberOfVouchers(Optional.empty(), newChildrenDateOfBirths, backDatedEntitlementDates);
        Integer vouchersFromPregnancy = calculateNumberOfVouchers(expectedDueDate, emptyList(), backDatedEntitlementDates);

        int backDatedVouchers = vouchersForChildren - vouchersFromPregnancy;
        // do not return negative vouchers
        return Math.max(backDatedVouchers, 0);
    }

    private int calculateNumberOfVouchers(Optional<LocalDate> expectedDueDate, List<LocalDate> newChildrenDateOfBirths, List<LocalDate> entitlementDates) {
        return entitlementDates.stream()
                .map(date -> entitlementCalculator.calculateVoucherEntitlement(expectedDueDate, newChildrenDateOfBirths, date))
                .mapToInt(VoucherEntitlement::getTotalVoucherEntitlement)
                .sum();
    }

    // get the list of entitlement dates that cover the new children.
    private List<LocalDate> getBackDatedEntitlementDates(List<LocalDate> newChildrenDatesOfBirth) {
        LocalDate earliestDateOfBirth = min(newChildrenDatesOfBirth);
        LocalDate rollingEntitlementDate = LocalDate.now();
        List<LocalDate> backDatedEntitlementDates = new ArrayList<>();

        while (rollingEntitlementDate.isAfter(earliestDateOfBirth)) {
            rollingEntitlementDate = rollingEntitlementDate.minusDays(entitlementCalculationDurationInDays);
            backDatedEntitlementDates.add(rollingEntitlementDate);
        }

        return backDatedEntitlementDates;
    }
}
