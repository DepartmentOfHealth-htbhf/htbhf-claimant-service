package uk.gov.dhsc.htbhf.claimant.model.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Constraint indicating that a String must not match a given regex pattern.
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = NotPatternValidator.class)
@Documented
public @interface NotPattern {

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
     * The regular expression that the value should not match.
     * @return regular expression - if the value matches the expression validation will fail
     */
    String regexp();

}
