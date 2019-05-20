package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.exception.EligibilityClientException;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.PersonDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.service.EligibilityClient.ELIGIBILITY_ENDPOINT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonDTOTestDataFactory.aValidPerson;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.NO_MATCH;

@ExtendWith(MockitoExtension.class)
class EligibilityClientTest {

    private static final String BASE_URI = "/v1/eligibility/";

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ClaimantToPersonDTOConverter claimantToPersonDTOConverter;

    private EligibilityClient client;

    @BeforeEach
    void setup() {
        client = new EligibilityClient(BASE_URI, restTemplate, claimantToPersonDTOConverter);
    }

    @Test
    void shouldCheckEligibilitySuccessfully() {
        Claimant claimant = aValidClaimant();
        PersonDTO person = aValidPerson();
        given(claimantToPersonDTOConverter.convert(any())).willReturn(person);
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        ResponseEntity<EligibilityResponse> response = new ResponseEntity<>(eligibilityResponse, HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(), eq(EligibilityResponse.class)))
                .willReturn(response);

        EligibilityResponse actualResponse = client.checkEligibility(claimant);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse).isEqualTo(eligibilityResponse);
        verify(claimantToPersonDTOConverter).convert(claimant);
        verify(restTemplate).postForEntity(BASE_URI + ELIGIBILITY_ENDPOINT, person, EligibilityResponse.class);
    }

    @Test
    void shouldCheckEligibilitySuccessfullyWhenNoMatchReturned() {
        Claimant claimant = aValidClaimant();
        PersonDTO person = aValidPerson();
        given(claimantToPersonDTOConverter.convert(any())).willReturn(person);
        given(restTemplate.postForEntity(anyString(), any(), eq(EligibilityResponse.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        EligibilityResponse actualResponse = client.checkEligibility(claimant);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getEligibilityStatus()).isEqualTo(NO_MATCH);
        verify(claimantToPersonDTOConverter).convert(claimant);
        verify(restTemplate).postForEntity(BASE_URI + ELIGIBILITY_ENDPOINT, person, EligibilityResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallNotOkOrNotFound() {
        Claimant claimant = aValidClaimant();
        PersonDTO person = aValidPerson();
        given(claimantToPersonDTOConverter.convert(any())).willReturn(person);
        ResponseEntity<EligibilityResponse> response = new ResponseEntity<>(anEligibilityResponseWithStatus(ELIGIBLE), HttpStatus.BAD_REQUEST);
        given(restTemplate.postForEntity(anyString(), any(), eq(EligibilityResponse.class)))
                .willReturn(response);

        EligibilityClientException thrown = catchThrowableOfType(() -> client.checkEligibility(claimant), EligibilityClientException.class);

        assertThat(thrown).as("Should throw an Exception when response code is not OK").isNotNull();
        assertThat(thrown.getMessage()).isEqualTo("Response code from Eligibility service was not OK, received: 400");
        verify(claimantToPersonDTOConverter).convert(claimant);
        verify(restTemplate).postForEntity(BASE_URI + ELIGIBILITY_ENDPOINT, person, EligibilityResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallReturnsError() {
        Claimant claimant = aValidClaimant();
        PersonDTO person = aValidPerson();
        given(claimantToPersonDTOConverter.convert(any())).willReturn(person);
        given(restTemplate.postForEntity(anyString(), any(), eq(EligibilityResponse.class)))
                .willThrow(new RestClientException("Test exception"));

        EligibilityClientException thrown = catchThrowableOfType(() -> client.checkEligibility(claimant), EligibilityClientException.class);

        assertThat(thrown).as("Should throw an Exception when post call returns error").isNotNull();
        assertThat(thrown.getMessage()).isEqualTo("Exception caught trying to call eligibility service at: " + BASE_URI + ELIGIBILITY_ENDPOINT);
        verify(claimantToPersonDTOConverter).convert(claimant);
        verify(restTemplate).postForEntity(BASE_URI + ELIGIBILITY_ENDPOINT, person, EligibilityResponse.class);
    }
}
