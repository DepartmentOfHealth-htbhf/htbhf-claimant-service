package uk.gov.dhsc.htbhf.claimant.service.v3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.exception.EligibilityClientException;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_NINO_V2;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithNino;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.PersonDTOV2TestDataFactory.aPersonDTOV2WithPregnantDependantDob;

@ExtendWith(MockitoExtension.class)
class EligibilityClientV3Test {

    private static final String BASE_URI = "http://localhost:8100";
    private static final String FULL_URI = "http://localhost:8100/v2/eligibility";
    private static final LocalDate NO_PREGNANT_DEPENDANT = null;

    @Mock
    private RestTemplate restTemplate;

    private EligibilityClientV3 client;

    @BeforeEach
    void setup() {
        client = new EligibilityClientV3(BASE_URI, restTemplate);
    }

    @Test
    void shouldCheckIdentityAndEligibilitySuccessfully() {
        Claimant claimant = aClaimantWithNino(HOMER_NINO_V2);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = CombinedIdAndEligibilityResponseTestDataFactory
                .anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        ResponseEntity<CombinedIdentityAndEligibilityResponse> response = new ResponseEntity<>(identityAndEligibilityResponse, HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(), eq(CombinedIdentityAndEligibilityResponse.class)))
                .willReturn(response);

        CombinedIdentityAndEligibilityResponse actualResponse = client.checkIdentityAndEligibility(claimant);

        assertThat(actualResponse).isEqualTo(identityAndEligibilityResponse);
        verify(restTemplate).postForEntity(FULL_URI, aPersonDTOV2WithPregnantDependantDob(NO_PREGNANT_DEPENDANT), CombinedIdentityAndEligibilityResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallNotOk() {
        Claimant claimant = aClaimantWithNino(HOMER_NINO_V2);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = CombinedIdAndEligibilityResponseTestDataFactory
                .anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        ResponseEntity<CombinedIdentityAndEligibilityResponse> response = new ResponseEntity<>(identityAndEligibilityResponse, HttpStatus.BAD_REQUEST);
        given(restTemplate.postForEntity(anyString(), any(), eq(CombinedIdentityAndEligibilityResponse.class)))
                .willReturn(response);

        EligibilityClientException thrown = catchThrowableOfType(() -> client.checkIdentityAndEligibility(claimant), EligibilityClientException.class);

        assertThat(thrown).as("Should throw an Exception when response code is not OK")
                .isNotNull()
                .hasMessage("Response code from Eligibility service was not OK, received: 400");
        verify(restTemplate).postForEntity(FULL_URI, aPersonDTOV2WithPregnantDependantDob(NO_PREGNANT_DEPENDANT), CombinedIdentityAndEligibilityResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallReturnsError() {
        Claimant claimant = aClaimantWithNino(HOMER_NINO_V2);
        RestClientException testException = new RestClientException("Test exception");
        given(restTemplate.postForEntity(anyString(), any(), eq(CombinedIdentityAndEligibilityResponse.class)))
                .willThrow(testException);

        EligibilityClientException thrown = catchThrowableOfType(() -> client.checkIdentityAndEligibility(claimant), EligibilityClientException.class);

        assertThat(thrown).as("Should throw an Exception when post call returns error")
                .isNotNull()
                .hasMessage("Exception caught trying to call eligibility service at: " + FULL_URI)
                .hasCause(testException);
        verify(restTemplate).postForEntity(FULL_URI, aPersonDTOV2WithPregnantDependantDob(NO_PREGNANT_DEPENDANT), CombinedIdentityAndEligibilityResponse.class);
    }

}
