package uk.gov.dhsc.htbhf.claimant.model.constraint;

import java.time.LocalDate;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DateWithinRelativeRangeValidator implements ConstraintValidator<DateWithinRelativeRange, LocalDate> {

    private int monthsInPast;
    private int monthsInFuture;

    @Override
    public void initialize(DateWithinRelativeRange constraintAnnotation) {
        this.monthsInPast = constraintAnnotation.monthsInPast();
        this.monthsInFuture = constraintAnnotation.monthsInFuture();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return isWithinValidRange(value);
    }

    private boolean isWithinValidRange(LocalDate value) {
        LocalDate now = LocalDate.now();
        LocalDate lastValidDate = now.plusMonths(monthsInFuture);
        boolean isBeforeEnd = value.isBefore(lastValidDate) || value.isEqual(lastValidDate);

        LocalDate firstValidDate = now.minusMonths(monthsInPast);
        boolean isAfterStart = value.isAfter(firstValidDate) || value.isEqual(firstValidDate);

        return isAfterStart && isBeforeEnd;
    }
}
