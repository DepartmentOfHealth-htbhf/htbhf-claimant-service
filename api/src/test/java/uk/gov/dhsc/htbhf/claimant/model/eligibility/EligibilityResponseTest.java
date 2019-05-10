package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityResponseTest {

    @ParameterizedTest
    @EnumSource(EligibilityStatus.class)
    void shouldCreateEligibilityResponseWithGivenStatus(EligibilityStatus eligibilityStatus) {
        EligibilityResponse eligibilityResponse = EligibilityResponse.buildWithStatus(eligibilityStatus);

        assertThat(eligibilityResponse.getEligibilityStatus()).isEqualTo(eligibilityStatus);
    }
}
