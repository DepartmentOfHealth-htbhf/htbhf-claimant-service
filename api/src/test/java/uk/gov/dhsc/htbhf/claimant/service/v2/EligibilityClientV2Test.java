package uk.gov.dhsc.htbhf.claimant.service.v2;

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
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.dwp.model.v2.PersonDTOV2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches;

@ExtendWith(MockitoExtension.class)
class EligibilityClientV2Test {

    private static final String BASE_URI = "http://localhost:8100";
    private static final String FULL_URI = "http://localhost:8100/v2/eligibility";

    @Mock
    private RestTemplate restTemplate;

    private EligibilityClientV2 client;

    @BeforeEach
    void setup() {
        client = new EligibilityClientV2(BASE_URI, restTemplate);
    }

    @Test
    void shouldCheckIdentityAndEligibilitySuccessfully() {
        Claimant claimant = aValidClaimant();
        IdentityAndEligibilityResponse identityAndEligibilityResponse = anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches();
        ResponseEntity<IdentityAndEligibilityResponse> response = new ResponseEntity<>(identityAndEligibilityResponse, HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(), eq(IdentityAndEligibilityResponse.class)))
                .willReturn(response);

        IdentityAndEligibilityResponse actualResponse = client.checkIdentityAndEligibility(claimant);

        assertThat(actualResponse).isEqualTo(identityAndEligibilityResponse);
        verify(restTemplate).postForEntity(FULL_URI, expectedPerson(), IdentityAndEligibilityResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallNotOk() {
        Claimant claimant = aValidClaimant();
        IdentityAndEligibilityResponse identityAndEligibilityResponse = anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches();
        ResponseEntity<IdentityAndEligibilityResponse> response = new ResponseEntity<>(identityAndEligibilityResponse, HttpStatus.BAD_REQUEST);
        given(restTemplate.postForEntity(anyString(), any(), eq(IdentityAndEligibilityResponse.class)))
                .willReturn(response);

        EligibilityClientException thrown = catchThrowableOfType(() -> client.checkIdentityAndEligibility(claimant), EligibilityClientException.class);

        assertThat(thrown).as("Should throw an Exception when response code is not OK")
                .isNotNull()
                .hasMessage("Response code from Eligibility service was not OK, received: 400");
        verify(restTemplate).postForEntity(FULL_URI, expectedPerson(), IdentityAndEligibilityResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallReturnsError() {
        Claimant claimant = aValidClaimant();
        RestClientException testException = new RestClientException("Test exception");
        given(restTemplate.postForEntity(anyString(), any(), eq(IdentityAndEligibilityResponse.class)))
                .willThrow(testException);

        EligibilityClientException thrown = catchThrowableOfType(() -> client.checkIdentityAndEligibility(claimant), EligibilityClientException.class);

        assertThat(thrown).as("Should throw an Exception when post call returns error")
                .isNotNull()
                .hasMessage("Exception caught trying to call eligibility service at: " + FULL_URI)
                .hasCause(testException);
        verify(restTemplate).postForEntity(FULL_URI, expectedPerson(), IdentityAndEligibilityResponse.class);
    }

    private PersonDTOV2 expectedPerson() {
        return PersonDTOV2.builder()
                .surname(VALID_LAST_NAME)
                .addressLine1(VALID_ADDRESS_LINE_1)
                .postcode(VALID_POSTCODE)
                .dateOfBirth(VALID_DOB)
                .nino(VALID_NINO)
                .mobilePhoneNumber(VALID_PHONE_NUMBER)
                .emailAddress(VALID_EMAIL_ADDRESS)
                .build();
    }

}
