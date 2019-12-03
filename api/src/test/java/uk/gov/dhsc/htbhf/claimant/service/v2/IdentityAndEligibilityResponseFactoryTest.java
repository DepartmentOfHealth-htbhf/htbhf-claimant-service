package uk.gov.dhsc.htbhf.claimant.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.ChildDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.claimant.service.v2.IdentityAndEligibilityResponseFactory.fromEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithChildrenAndStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.childrenWithBirthdates;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.TWO_CHILDREN;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.addHouseholdIdentifier;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anAllMatchedEligibilityConfirmedUCResponseWithHouseholdIdentifier;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchFailedResponse;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityNotConfirmedResponse;

class IdentityAndEligibilityResponseFactoryTest {

    @Test
    void shouldBuildMatchedResponseFromEligibilityResponse() {
        //Given
        List<ChildDTO> children = childrenWithBirthdates(TWO_CHILDREN);
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithChildrenAndStatus(children, EligibilityStatus.ELIGIBLE);
        //When
        IdentityAndEligibilityResponse identityAndEligibilityResponse = fromEligibilityResponse(eligibilityResponse);
        //Then
        IdentityAndEligibilityResponse expectedResponse = anAllMatchedEligibilityConfirmedUCResponseWithHouseholdIdentifier(TWO_CHILDREN,
                DWP_HOUSEHOLD_IDENTIFIER);
        assertThat(identityAndEligibilityResponse).isEqualTo(expectedResponse);
    }

    @Test
    void shouldBuildNotMatchedResponseFromEligibilityResponse() {
        //Given
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(EligibilityStatus.INELIGIBLE);
        //When
        IdentityAndEligibilityResponse identityAndEligibilityResponse = fromEligibilityResponse(eligibilityResponse);
        //Then
        IdentityAndEligibilityResponse expectedResponse = addHouseholdIdentifier(anIdentityMatchedEligibilityNotConfirmedResponse(), DWP_HOUSEHOLD_IDENTIFIER);
        assertThat(identityAndEligibilityResponse).isEqualTo(expectedResponse);
    }

    @ValueSource(strings = {"PENDING", "NO_MATCH", "ERROR", "DUPLICATE"})
    @ParameterizedTest
    void shouldBuildNotMatchedResponseFromEligibilityResponse(EligibilityStatus eligibilityStatus) {
        //Given
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(eligibilityStatus);
        //When
        IdentityAndEligibilityResponse identityAndEligibilityResponse = fromEligibilityResponse(eligibilityResponse);
        //Then
        IdentityAndEligibilityResponse expectedResponse = addHouseholdIdentifier(anIdentityMatchFailedResponse(), DWP_HOUSEHOLD_IDENTIFIER);
        assertThat(identityAndEligibilityResponse).isEqualTo(expectedResponse);
    }

}
