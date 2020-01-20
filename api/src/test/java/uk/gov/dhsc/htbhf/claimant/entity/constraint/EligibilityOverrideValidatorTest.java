package uk.gov.dhsc.htbhf.claimant.entity.constraint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.util.stream.Stream;
import javax.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityOverrideTestDataFactory.aConfirmedEligibilityOverride;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.OVERRIDE_UNTIL_FIVE_YEARS;

public class EligibilityOverrideValidatorTest {

    private static final ConstraintValidatorContext NULL_CONTEXT = null;

    EligibilityOverrideValidator eligibilityOverrideValidator = new EligibilityOverrideValidator();

    @Test
    void shouldValidateNullEligibilityOverride() {
        EligibilityOverride nullEligibilityOverride = null;
        boolean result = eligibilityOverrideValidator.isValid(nullEligibilityOverride, NULL_CONTEXT);

        assertThat(result).isTrue();
    }

    @Test
    void shouldValidateValidEligibilityOverride() {
        boolean result = eligibilityOverrideValidator.isValid(aConfirmedEligibilityOverride(), NULL_CONTEXT);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource("eligibilityOverrideWithNullValues")
    void shouldFailToValidateEligibilityOverrideWithNullValues(EligibilityOverride eligibilityOverride) {
        boolean result = eligibilityOverrideValidator.isValid(eligibilityOverride, NULL_CONTEXT);

        assertThat(result).isFalse();
    }

    public static Stream<EligibilityOverride> eligibilityOverrideWithNullValues() {
        return Stream.of(
                EligibilityOverride.builder()
                        .eligibilityOutcome(null)
                        .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                        .build(),
                EligibilityOverride.builder()
                        .eligibilityOutcome(EligibilityOutcome.CONFIRMED)
                        .overrideUntil(null)
                        .build(),
                EligibilityOverride.builder()
                        .eligibilityOutcome(null)
                        .overrideUntil(null)
                        .build()
        );
    }
}