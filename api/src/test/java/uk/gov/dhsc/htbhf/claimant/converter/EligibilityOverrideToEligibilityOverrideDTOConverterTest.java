package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.claimant.model.EligibilityOverrideDTO;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_AND_LISA_DOBS;
import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityOverrideTestDataFactory.aConfirmedEligibilityForPregnantTeenager;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityOverrideTestDataFactory.aConfirmedEligibilityOverrideWithChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.OVERRIDE_UNTIL_FIVE_YEARS;
import static uk.gov.dhsc.htbhf.dwp.model.QualifyingBenefits.UNDER_18;

class EligibilityOverrideToEligibilityOverrideDTOConverterTest {

    private EligibilityOverrideToEligibilityOverrideDTOConverter converter = new EligibilityOverrideToEligibilityOverrideDTOConverter();

    @Test
    void shouldConvertEligibilityOverrideWithChildrenToEligibilityOverrideDTO() {
        //Given
        EligibilityOverride eligibilityOverride = aConfirmedEligibilityOverrideWithChildren(MAGGIE_AND_LISA_DOBS);

        //When
        EligibilityOverrideDTO result = converter.convert(eligibilityOverride);

        //Then
        assertThat(result).isNotNull();
        assertThat(result.getEligibilityOutcome()).isEqualTo(EligibilityOutcome.CONFIRMED);
        assertThat(result.getChildrenDob()).isEqualTo(MAGGIE_AND_LISA_DOBS);
        assertThat(result.getOverrideUntil()).isEqualTo(OVERRIDE_UNTIL_FIVE_YEARS);
    }

    @Test
    void shouldConvertEligibilityOverrideForPregnantTeenagerToEligibilityOverrideDTO() {
        //Given
        EligibilityOverride eligibilityOverride = aConfirmedEligibilityForPregnantTeenager(OVERRIDE_UNTIL_FIVE_YEARS);

        //When
        EligibilityOverrideDTO result = converter.convert(eligibilityOverride);

        //Then
        assertThat(result).isNotNull();
        assertThat(result.getEligibilityOutcome()).isEqualTo(EligibilityOutcome.CONFIRMED);
        assertThat(result.getChildrenDob()).isEqualTo(NO_CHILDREN);
        assertThat(result.getOverrideUntil()).isEqualTo(OVERRIDE_UNTIL_FIVE_YEARS);
        assertThat(result.getQualifyingBenefits()).isEqualTo(UNDER_18);
    }

    @Test
    void shouldCovertNullEligibilityOverrideDTO() {
        EligibilityOverrideDTO result = converter.convert(null);
        assertThat(result).isNull();
    }
}