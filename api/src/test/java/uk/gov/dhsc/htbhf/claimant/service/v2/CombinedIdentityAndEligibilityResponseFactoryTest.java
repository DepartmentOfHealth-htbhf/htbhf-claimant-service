package uk.gov.dhsc.htbhf.claimant.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.ChildDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.service.v2.CombinedIdentityAndEligibilityResponseFactory.fromEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithChildrenAndStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.childrenWithBirthdates;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.TWO_CHILDREN;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdentityAndEligibilityResponseTestDataFactory.anIdentityMatchFailedResponse;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityNotConfirmedResponse;

class CombinedIdentityAndEligibilityResponseFactoryTest {

    @Test
    void shouldBuildMatchedResponseFromEligibilityResponse() {
        //Given
        List<ChildDTO> children = childrenWithBirthdates(TWO_CHILDREN);
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithChildrenAndStatus(children, EligibilityStatus.ELIGIBLE);
        //When
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = fromEligibilityResponse(eligibilityResponse);
        //Then
        CombinedIdentityAndEligibilityResponse expectedResponse = anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(TWO_CHILDREN);
        assertThat(identityAndEligibilityResponse).isEqualTo(expectedResponse);
    }

    @Test
    void shouldBuildNotMatchedResponseFromEligibilityResponse() {
        //Given
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(EligibilityStatus.INELIGIBLE);
        //When
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = fromEligibilityResponse(eligibilityResponse);
        //Then
        CombinedIdentityAndEligibilityResponse expectedResponse = anIdentityMatchedEligibilityNotConfirmedResponse();
        assertThat(identityAndEligibilityResponse).isEqualTo(expectedResponse);
    }

    @ValueSource(strings = {"PENDING", "NO_MATCH", "ERROR", "DUPLICATE"})
    @ParameterizedTest
    void shouldBuildNotMatchedResponseFromEligibilityResponse(EligibilityStatus eligibilityStatus) {
        //Given
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(eligibilityStatus);
        //When
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = fromEligibilityResponse(eligibilityResponse);
        //Then
        CombinedIdentityAndEligibilityResponse expectedResponse = anIdentityMatchFailedResponse();
        assertThat(identityAndEligibilityResponse).isEqualTo(expectedResponse);
    }

}
