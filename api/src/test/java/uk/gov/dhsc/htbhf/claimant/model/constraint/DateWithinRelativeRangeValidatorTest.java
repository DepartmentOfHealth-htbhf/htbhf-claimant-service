package uk.gov.dhsc.htbhf.claimant.model.constraint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DateWithinRelativeRangeValidatorTest {

    @Mock
    DateWithinRelativeRange constraint;

    @InjectMocks
    DateWithinRelativeRangeValidator validator;

    @Test
    void todayShouldBeValid() {
        // Given
        given(constraint.monthsInPast()).willReturn(1);
        given(constraint.monthsInFuture()).willReturn(8);
        validator.initialize(constraint);

        LocalDate value = LocalDate.now();

        // When
        boolean result = validator.isValid(value, null);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void dateAtStartShouldBeValid() {
        // Given
        given(constraint.monthsInPast()).willReturn(1);
        given(constraint.monthsInFuture()).willReturn(8);
        validator.initialize(constraint);

        LocalDate value = LocalDate.now().minusMonths(1);

        // When
        boolean result = validator.isValid(value, null);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void dateAtEndShouldBeValid() {
        // Given
        given(constraint.monthsInPast()).willReturn(1);
        given(constraint.monthsInFuture()).willReturn(8);
        validator.initialize(constraint);

        LocalDate value = LocalDate.now().plusMonths(8);

        // When
        boolean result = validator.isValid(value, null);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void dateAfterEndShouldNotBeValid() {
        // Given
        given(constraint.monthsInPast()).willReturn(1);
        given(constraint.monthsInFuture()).willReturn(8);
        validator.initialize(constraint);

        LocalDate value = LocalDate.now().plusMonths(8).plusDays(1);

        // When
        boolean result = validator.isValid(value, null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void dateBeforeStartShouldNotBeValid() {
        // Given
        given(constraint.monthsInPast()).willReturn(1);
        given(constraint.monthsInFuture()).willReturn(8);
        validator.initialize(constraint);

        LocalDate value = LocalDate.now().minusMonths(1).minusDays(1);

        // When
        boolean result = validator.isValid(value, null);

        // Then
        assertThat(result).isFalse();
    }

}