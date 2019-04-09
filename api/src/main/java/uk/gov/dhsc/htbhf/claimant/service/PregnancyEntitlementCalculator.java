package uk.gov.dhsc.htbhf.claimant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PregnancyEntitlementCalculator {

    private final int pregnancyGracePeriodInDays;

    public PregnancyEntitlementCalculator(@Value("${entitlement.pregnancy-grace-period-in-days}") int pregnancyGracePeriodInDays) {
        this.pregnancyGracePeriodInDays = pregnancyGracePeriodInDays;
    }

    public boolean isEntitledToVoucher(LocalDate dueDate) {
        LocalDate endOfGracePeriod = dueDate.plusDays(pregnancyGracePeriodInDays);
        return !endOfGracePeriod.isBefore(LocalDate.now());
    }
}
