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
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.PersonDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.service.EligibilityClient.ELIGIBILITY_ENDPOINT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
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
        Claimant claimant = aValidClaimant();
        PersonDTO person = aValidPerson();
        given(claimantToPersonDTOConverter.convert(any())).willReturn(person);
        ResponseEntity<EligibilityResponse> response = new ResponseEntity<>(anEligibilityResponse(), HttpStatus.OK);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class)))
                .willReturn(response);

        EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);

        assertThat(eligibilityResponse).isNotNull();
        assertThat(eligibilityResponse.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        verify(claimantToPersonDTOConverter).convert(claimant);
        verify(restTemplateWithIdHeaders).postForEntity(baseUri + ELIGIBILITY_ENDPOINT, person, EligibilityResponse.class);
    }
}
