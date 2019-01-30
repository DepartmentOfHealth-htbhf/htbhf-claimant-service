package uk.gov.dhsc.htbhf.claimant.model.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.LocalDate;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = DateWithinRelativeRangeValidator.class)
@Documented
public @interface DateWithinRelativeRange {

    /**
     * The message to use when this constraint is violated.
     *
     * @return The message to use when this constraint is violated
     */
    String message();

    /**
     * The validation groups to which this constraint belongs.
     *
     * @return The validation groups to which this constraint belongs
     */
    Class<?>[] groups() default {};

    /**
     * Payload can be used by clients of the Bean Validation API to assign custom payload objects to a constraint. This attribute is not used by the API itself.
     *
     * @return the custom payload
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * The maximum number of months in the past a date is allowed to be.
     *
     * @return The maximum number of months in the past a date is allowed to be
     */
    int monthsInPast();

    /**
     * The maximum number of months in the future a date is allowed to be.
     *
     * @return The maximum number of months in the future a date is allowed to be
     */
    int monthsInFuture();
}

class DateWithinRelativeRangeValidator implements ConstraintValidator<DateWithinRelativeRange, LocalDate> {

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
