package uk.gov.dhsc.htbhf.claimant.model.constraint;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class NotPatternValidator implements ConstraintValidator<NotPattern, String> {

    private Pattern pattern;

    @Override
    public void initialize(NotPattern constraintAnnotation) {
        this.pattern = Pattern.compile(constraintAnnotation.regexp());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (isEmpty(value)) {
            return true;
        }
        return !pattern.matcher(value).matches();
    }
}
