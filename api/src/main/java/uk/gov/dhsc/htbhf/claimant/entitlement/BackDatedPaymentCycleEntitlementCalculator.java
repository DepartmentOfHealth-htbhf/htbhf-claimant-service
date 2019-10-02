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
 * The expected due date and the birthdays of children resulting from the pregnancy are used to
 * calculate the difference between the number of vouchers that were received for pregnancy and
 * the number of vouchers that should have been received for the new children. If the difference in vouchers
 * is less than zero, zero is returned.
 */
@Service
public class BackDatedPaymentCycleEntitlementCalculator {

    private final Integer entitlementCalculationDurationInDays;
    private final EntitlementCalculator entitlementCalculator;

    /**
     * Constructor for {@link BackDatedPaymentCycleEntitlementCalculator}.
     *
     * @param paymentCycleConfig    configuration for the payment cycle
     * @param entitlementCalculator calculates entitlement for a single calculation period
     */
    public BackDatedPaymentCycleEntitlementCalculator(PaymentCycleConfig paymentCycleConfig,
                                                      EntitlementCalculator entitlementCalculator) {
        this.entitlementCalculationDurationInDays = paymentCycleConfig.getEntitlementCalculationDurationInDays();
        this.entitlementCalculator = entitlementCalculator;
    }

    /**
     * Calculates the back dated vouchers given the expected due date and the dates of births resulting from the pregnancy.
     *
     * @param expectedDueDate         expected due date
     * @param newChildrenDateOfBirths the dates of births of the children resulting from the pregnancy
     * @param cycleStartDate          the start date of the payment cycle
     * @return the number of back dated vouchers the claimant is entitled to
     */
    public int calculateBackDatedVouchers(Optional<LocalDate> expectedDueDate, List<LocalDate> newChildrenDateOfBirths, LocalDate cycleStartDate) {
        List<LocalDate> backDatedEntitlementDates = getBackDatedEntitlementDates(newChildrenDateOfBirths, cycleStartDate);
        int vouchersForChildren = calculateNumberOfVouchers(Optional.empty(), newChildrenDateOfBirths, backDatedEntitlementDates);
        int vouchersFromPregnancy = calculateNumberOfVouchers(expectedDueDate, emptyList(), backDatedEntitlementDates);

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

    // get the list of entitlement dates since the oldest child was born
    private List<LocalDate> getBackDatedEntitlementDates(List<LocalDate> newChildrenDatesOfBirth, LocalDate cycleStartDate) {
        LocalDate earliestDateOfBirth = min(newChildrenDatesOfBirth);
        LocalDate rollingEntitlementDate = cycleStartDate.minusDays(entitlementCalculationDurationInDays);
        List<LocalDate> backDatedEntitlementDates = new ArrayList<>();

        while (rollingEntitlementDate.isAfter(earliestDateOfBirth) || rollingEntitlementDate.isEqual(earliestDateOfBirth)) {
            backDatedEntitlementDates.add(rollingEntitlementDate);
            rollingEntitlementDate = rollingEntitlementDate.minusDays(entitlementCalculationDurationInDays);
        }

        return backDatedEntitlementDates;
    }
}
