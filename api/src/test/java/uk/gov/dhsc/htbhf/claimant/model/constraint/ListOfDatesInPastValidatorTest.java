package uk.gov.dhsc.htbhf.claimant.model.constraint;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListOfDatesInPastValidatorTest {

    private ListOfDatesInPastValidator validator = new ListOfDatesInPastValidator();

    @Test
    void shouldValidateEmptyList() {
        //Given
        List<LocalDate> dates = new ArrayList<>();
        //When
        boolean isValid = validator.isValid(dates, null);
        //Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateNullList() {
        //Given
        List<LocalDate> dates = null;
        //When
        boolean isValid = validator.isValid(dates, null);
        //Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateListWithDateInPast() {
        //Given
        LocalDate dateInPast = LocalDate.now().minusDays(1);
        List<LocalDate> dates = Collections.singletonList(dateInPast);
        //When
        boolean isValid = validator.isValid(dates, null);
        //Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldNotValidateListWithDateInFuture() {
        //Given
        LocalDate dateInFuture = LocalDate.now().plusDays(1);
        List<LocalDate> dates = Collections.singletonList(dateInFuture);
        //When
        boolean isValid = validator.isValid(dates, null);
        //Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldNotValidateListWithDateInFutureAndPast() {
        //Given
        LocalDate dateInFuture = LocalDate.now().plusDays(1);
        LocalDate dateInPast = LocalDate.now().minusDays(1);
        List<LocalDate> dates = List.of(dateInFuture, dateInPast);
        //When
        boolean isValid = validator.isValid(dates, null);
        //Then
        assertThat(isValid).isFalse();
    }

}
