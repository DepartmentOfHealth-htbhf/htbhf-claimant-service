package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.service.EligibilityOutcomeToEligibilityStatusConverter.fromEligibilityOutcome;

class EligibilityOutcomeToEligibilityStatusConverterTest {

    @ParameterizedTest(name = "Should convert from EligibilityOutcome {0} to EligibilityStatus {1}")
    @CsvSource({
            "CONFIRMED, ELIGIBLE",
            "NOT_CONFIRMED, INELIGIBLE",
            "NOT_SET, NO_MATCH"
    })
    void shouldConvertFromEligibilityOutcome(EligibilityOutcome eligibilityOutcome, EligibilityStatus eligibilityStatus) {
        assertThat(fromEligibilityOutcome(eligibilityOutcome)).isEqualTo(eligibilityStatus);
    }
}
