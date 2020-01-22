package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.claimant.model.EligibilityOverrideDTO;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_AND_LISA_DOBS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityOverrideTestDataFactory.aConfirmedEligibilityOverrideWithChildren;

class EligibilityOverrideToEligibilityOverrideDTOConverterTest {

    private EligibilityOverrideToEligibilityOverrideDTOConverter converter = new EligibilityOverrideToEligibilityOverrideDTOConverter();

    @Test
    void shouldConvertEligibilityOverrideToEligibilityOverrideDTO() {
        //Given
        EligibilityOverride eligibilityOverride = aConfirmedEligibilityOverrideWithChildren(MAGGIE_AND_LISA_DOBS);

        //When
        EligibilityOverrideDTO result = converter.convert(eligibilityOverride);

        //Then
        assertThat(result).isNotNull();
        assertThat(result.getEligibilityOutcome()).isEqualTo(eligibilityOverride.getEligibilityOutcome());
        assertThat(result.getChildrenDob()).isEqualTo(MAGGIE_AND_LISA_DOBS);
        assertThat(result.getOverrideUntil()).isEqualTo(eligibilityOverride.getOverrideUntil());
    }

    @Test
    void shouldCovertNullEligibilityOverrideDTO() {
        EligibilityOverrideDTO result = converter.convert(null);
        assertThat(result).isNull();
    }
}