package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Responsible for deciding whether a claimant is entitled to a voucher for pregnancy,
 * by comparing the due date to today's date.
 * There is a grace period after the due date before the claimant stops being eligible for a voucher.
 */
@Component
public class PregnancyEntitlementCalculator {

    private final int pregnancyGracePeriodInDays;

    public PregnancyEntitlementCalculator(@Value("${entitlement.pregnancy-grace-period-in-days}") int pregnancyGracePeriodInDays) {
        this.pregnancyGracePeriodInDays = pregnancyGracePeriodInDays;
    }

    public boolean isEntitledToVoucher(LocalDate dueDate) {
        if (dueDate == null) {
            return false;
        }
        LocalDate endOfGracePeriod = dueDate.plusDays(pregnancyGracePeriodInDays);
        return !endOfGracePeriod.isBefore(LocalDate.now());
    }
}
