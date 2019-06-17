package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates the number of additional pregnancy vouchers when a claim has been updated to include an expected delivery date.
 */
@Component
public class AdditionalPregnancyVoucherCalculator {

    private final Integer entitlementCalculationDurationInDays;
    private final PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    private final int vouchersPerPregnancy;

    public AdditionalPregnancyVoucherCalculator(@Value("${entitlement.number-of-vouchers-per-pregnancy}") int vouchersPerPregnancy,
                                                PaymentCycleConfig paymentCycleConfig,
                                                PregnancyEntitlementCalculator pregnancyEntitlementCalculator) {
        this.entitlementCalculationDurationInDays = paymentCycleConfig.getEntitlementCalculationDurationInDays();
        this.pregnancyEntitlementCalculator = pregnancyEntitlementCalculator;
        this.vouchersPerPregnancy = vouchersPerPregnancy;
    }

    /**
     * Get the number of additional pregnancy vouchers when we've been notified that a claimant is now pregnant.
     * The pregnancy entitlement calculator is called for each full week that is left of the current cycle.
     * e.g if the claim was updated mid way through week two of the cycle, the user will get pregnancy vouchers for week 3 and 4 (given a four week cycle).
     * if the claim was updated during week four of the cycle, no additional vouchers will be returned.
     *
     * @param expectedDueDate  the expected due date of the claimant
     * @param paymentCycle     the current payment cycle
     * @param claimUpdatedDate the date that the claim was updated with the new expected due date
     * @return the number of additional pregnancy vouchers the claimant is entitled to
     */
    public int getAdditionalPregnancyVouchers(LocalDate expectedDueDate,
                                              PaymentCycle paymentCycle,
                                              LocalDate claimUpdatedDate) {
        if (!claimUpdatedDate.isAfter(paymentCycle.getCycleStartDate())) {
            // if the update happened on the same day as the payment cycle start date,
            // then no need for additional vouchers as the payment cycle will now include pregnancy vouchers.
            return 0;
        }

        List<LocalDate> entitlementDates = getEntitlementDates(paymentCycle, claimUpdatedDate);
        return entitlementDates.stream()
                .map(entitlementDate -> pregnancyEntitlementCalculator.isEntitledToVoucher(expectedDueDate, entitlementDate))
                .mapToInt(isEntitled -> isEntitled ? vouchersPerPregnancy : 0)
                .sum();
    }

    private List<LocalDate> getEntitlementDates(PaymentCycle paymentCycle, LocalDate claimUpdatedDate) {
        LocalDate rollingEntitlementDate = getFirstEntitlementDateOnOrAfterClaimUpdatedDate(paymentCycle.getCycleStartDate(), claimUpdatedDate);

        List<LocalDate> entitlementDates = new ArrayList<>();
        while (rollingEntitlementDate.isBefore(paymentCycle.getCycleEndDate())) {
            entitlementDates.add(rollingEntitlementDate);
            rollingEntitlementDate = rollingEntitlementDate.plusDays(entitlementCalculationDurationInDays);
        }

        return entitlementDates;
    }

    private LocalDate getFirstEntitlementDateOnOrAfterClaimUpdatedDate(LocalDate paymentCycleStartDate, LocalDate claimUpdatedDate) {
        LocalDate firstEntitlementDate = paymentCycleStartDate;
        while (firstEntitlementDate.isBefore(claimUpdatedDate)) {
            firstEntitlementDate = firstEntitlementDate.plusDays(entitlementCalculationDurationInDays);
        }
        return firstEntitlementDate;
    }
}
