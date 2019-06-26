package uk.gov.dhsc.htbhf.claimant.model.constraint;

import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ListOfDatesInPastValidator implements ConstraintValidator<ListOfDatesInPast, List<LocalDate>> {

    @Override
    public boolean isValid(List<LocalDate> dates, ConstraintValidatorContext context) {
        if (CollectionUtils.isEmpty(dates)) {
            return true;
        }
        return allDatesInPast(dates);
    }

    private boolean allDatesInPast(List<LocalDate> dates) {
        LocalDate now = LocalDate.now();

        for (LocalDate date : dates) {
            if (now.isBefore(date)) {
                return false;
            }
        }
        return true;
    }
}
