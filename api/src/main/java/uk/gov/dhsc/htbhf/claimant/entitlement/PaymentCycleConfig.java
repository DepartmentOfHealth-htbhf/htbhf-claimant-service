package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class PaymentCycleConfig {

    private final Integer weeksBeforeDueDate;
    private final Integer weeksAfterDueDate;
    private final Integer entitlementCalculationDurationInDays;
    private final Integer numberOfCalculationPeriods;

    public PaymentCycleConfig(@Value("${payment-cycle.cycle-duration-in-days}") Integer paymentCycleDurationInDays,
                              @Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods,
                              @Value("${payment-cycle.child-matched-to-pregnancy-period.weeks-before-due-date}") Integer weeksBeforeDueDate,
                              @Value("${payment-cycle.child-matched-to-pregnancy-period.weeks-after-due-date}") Integer weeksAfterDueDate) {
        validateArguments(paymentCycleDurationInDays, numberOfCalculationPeriods);
        this.weeksBeforeDueDate = weeksBeforeDueDate;
        this.weeksAfterDueDate = weeksAfterDueDate;
        this.entitlementCalculationDurationInDays = paymentCycleDurationInDays / numberOfCalculationPeriods;
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
    }

    private void validateArguments(Integer paymentCycleDurationInDays, Integer numberOfCalculationPeriods) {
        if (paymentCycleDurationInDays <= 0) {
            throw new IllegalArgumentException("Payment cycle duration must be greater than zero");
        }
        if (numberOfCalculationPeriods <= 0) {
            throw new IllegalArgumentException("Number of calculation periods must be greater than zero");
        }
        if (paymentCycleDurationInDays % numberOfCalculationPeriods != 0) {
            throw new IllegalArgumentException("Payment cycle duration of " + paymentCycleDurationInDays
                    + " days is not divisible by number of calculation periods " + numberOfCalculationPeriods);
        }
    }
}
