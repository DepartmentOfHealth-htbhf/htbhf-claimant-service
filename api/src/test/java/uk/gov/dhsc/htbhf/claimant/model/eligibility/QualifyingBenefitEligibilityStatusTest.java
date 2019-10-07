package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class QualifyingBenefitEligibilityStatusTest {

    @ParameterizedTest(name = "should provide qualifying benefit status of {1} for eligibility status is {0}")
    @CsvSource({
            "ELIGIBLE, CONFIRMED",
            "INELIGIBLE, NOT_CONFIRMED",
            "PENDING, NOT_SET",
            "NO_MATCH, NOT_SET",
            "ERROR, NOT_SET",
            "DUPLICATE, NOT_SET"
    })
    void shouldProvideCorrectQualifyingBenefitStatus(EligibilityStatus source, QualifyingBenefitEligibilityStatus expected) {
        QualifyingBenefitEligibilityStatus actual = QualifyingBenefitEligibilityStatus.fromEligibilityStatus(source);

        assertThat(actual).isEqualTo(expected);
    }


}