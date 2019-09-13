package uk.gov.dhsc.htbhf.claimant.model.constraint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotPatternValidatorTest {

    @Mock
    NotPattern constraint;

    @InjectMocks
    NotPatternValidator validator;

    @Test
    void nullStringShouldBeValid() {
        given(constraint.regexp()).willReturn(".*");
        validator.initialize(constraint);

        boolean result = validator.isValid(null, null);

        assertThat(result).isTrue();
    }

    @Test
    void emptyStringShouldBeValid() {
        given(constraint.regexp()).willReturn(".*");
        validator.initialize(constraint);

        boolean result = validator.isValid("", null);

        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "String ('{0}') matching pattern should not be valid")
    @ValueSource(strings = {
            "AB",
            "ab1234",
            "Aba",
            "aB1"
    })
    void stringMatchingPatternShouldNotBeValid(String value) {
        given(constraint.regexp()).willReturn("[Aa][Bb].*");
        validator.initialize(constraint);

        boolean result = validator.isValid(value, null);

        assertThat(result).isFalse();
    }

    @ParameterizedTest(name = "String ('{0}') not matching pattern should be valid")
    @ValueSource(strings = {
            "AC",
            "A",
            "aAba",
            "b"
    })
    void stringNotMatchingPatternShouldBeValid(String value) {
        given(constraint.regexp()).willReturn("[Aa][Bb].*");
        validator.initialize(constraint);

        boolean result = validator.isValid(value, null);

        assertThat(result).isTrue();
    }
}