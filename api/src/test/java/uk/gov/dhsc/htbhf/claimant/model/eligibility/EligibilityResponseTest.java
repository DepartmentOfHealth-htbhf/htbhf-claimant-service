package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse.buildWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithChildren;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.LISA_DOB;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.MAGGIE_DATE_OF_BIRTH;

class EligibilityResponseTest {

    @ParameterizedTest
    @EnumSource(EligibilityStatus.class)
    void shouldCreateEligibilityResponseWithGivenStatus(EligibilityStatus eligibilityStatus) {
        EligibilityResponse eligibilityResponse = buildWithStatus(eligibilityStatus);

        assertThat(eligibilityResponse.getEligibilityStatus()).isEqualTo(eligibilityStatus);
    }

    @Test
    void shouldGetDateOfBirthOfChildren() {
        EligibilityResponse eligibilityResponse = anEligibilityResponse();

        List<LocalDate> dateOfBirthOfChildren = eligibilityResponse.getDateOfBirthOfChildren();

        assertThat(dateOfBirthOfChildren).isNotEmpty();
        assertThat(dateOfBirthOfChildren).hasSize(2);
        assertThat(dateOfBirthOfChildren).containsExactly(MAGGIE_DATE_OF_BIRTH, LISA_DOB);
    }

    @Test
    void shouldReturnEmptyListForDateOfBirthOfChildrenWhenNoChildren() {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithChildren(emptyList());

        List<LocalDate> dateOfBirthOfChildren = eligibilityResponse.getDateOfBirthOfChildren();

        assertThat(dateOfBirthOfChildren).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForDateOfBirthOfChildrenWhenNullChildren() {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithChildren(null);

        List<LocalDate> dateOfBirthOfChildren = eligibilityResponse.getDateOfBirthOfChildren();

        assertThat(dateOfBirthOfChildren).isEmpty();
    }
}
