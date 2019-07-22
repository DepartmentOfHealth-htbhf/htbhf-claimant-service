package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Responsible for deciding whether a claimant is entitled to a voucher for pregnancy,
 * by comparing the due date to a given entitlement date.
 * There is a grace period after the due date before the claimant stops being eligible for a voucher.
 */
@Component
public class PregnancyEntitlementCalculator {

    private final int pregnancyGracePeriodInWeeks;

    public PregnancyEntitlementCalculator(@Value("${entitlement.pregnancy-grace-period-in-weeks}") int pregnancyGracePeriodInWeeks) {
        this.pregnancyGracePeriodInWeeks = pregnancyGracePeriodInWeeks;
    }

    public boolean isEntitledToVoucher(LocalDate dueDate, LocalDate entitlementDate) {
        if (entitlementDate == null) {
            throw new IllegalArgumentException("entitlementDate must not be null");
        }
        if (dueDate == null) {
            return false;
        }
        LocalDate endOfGracePeriod = dueDate.plusWeeks(pregnancyGracePeriodInWeeks);
        return !endOfGracePeriod.isBefore(entitlementDate);
    }
}
