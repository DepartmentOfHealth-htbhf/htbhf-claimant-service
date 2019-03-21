package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithoutEligibilityStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonDTOTestDataFactory.aValidPerson;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class EligibilityClientTest {

    @MockBean
    private RestTemplate restTemplateWithIdHeaders;
    @MockBean
    private ClaimantToPersonDTOConverter claimantToPersonDTOConverter;
    @Value("${eligibility.base-uri}")
    private String baseUri;

    @Autowired
    private EligibilityClient client;

    @Test
    void shouldCheckEligibilitySuccessfully() {
        Claimant claimant = aClaimantWithoutEligibilityStatus();
        PersonDTO person = aValidPerson();
        given(claimantToPersonDTOConverter.convert(any())).willReturn(person);
        EligibilityResponse eligibilityResponse = anEligibilityResponse();
        ResponseEntity<EligibilityResponse> response = new ResponseEntity<>(eligibilityResponse, HttpStatus.OK);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class)))
                .willReturn(response);

        EligibilityResponse actualResponse = client.checkEligibility(claimant);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse).isEqualTo(eligibilityResponse);
        verify(claimantToPersonDTOConverter).convert(claimant);
        verify(restTemplateWithIdHeaders).postForEntity(baseUri + ELIGIBILITY_ENDPOINT, person, EligibilityResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallNotOk() {
        Claimant claimant = aClaimantWithoutEligibilityStatus();
        PersonDTO person = aValidPerson();
        given(claimantToPersonDTOConverter.convert(any())).willReturn(person);
        ResponseEntity<EligibilityResponse> response = new ResponseEntity<>(anEligibilityResponse(), HttpStatus.BAD_REQUEST);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class)))
                .willReturn(response);

        EligibilityClientException thrown = catchThrowableOfType(() -> client.checkEligibility(claimant), EligibilityClientException.class);

        assertThat(thrown).as("Should throw an Exception when response code is not OK").isNotNull();
        assertThat(thrown.getMessage()).isEqualTo("Response code from Eligibility service was not OK, received: 400");
        verify(claimantToPersonDTOConverter).convert(claimant);
        verify(restTemplateWithIdHeaders).postForEntity(baseUri + ELIGIBILITY_ENDPOINT, person, EligibilityResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallReturnsError() {
        Claimant claimant = aClaimantWithoutEligibilityStatus();
        PersonDTO person = aValidPerson();
        given(claimantToPersonDTOConverter.convert(any())).willReturn(person);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class)))
                .willThrow(new RestClientException("Test exception"));

        EligibilityClientException thrown = catchThrowableOfType(() -> client.checkEligibility(claimant), EligibilityClientException.class);

        assertThat(thrown).as("Should throw an Exception when post call returns error").isNotNull();
        assertThat(thrown.getMessage()).isEqualTo("Exception caught trying to call eligibility service at: " + baseUri + ELIGIBILITY_ENDPOINT);
        verify(claimantToPersonDTOConverter).convert(claimant);
        verify(restTemplateWithIdHeaders).postForEntity(baseUri + ELIGIBILITY_ENDPOINT, person, EligibilityResponse.class);
    }
}
