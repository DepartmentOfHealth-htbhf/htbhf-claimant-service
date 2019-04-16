package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.exception.CardClientException;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aValidCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;

@SpringBootTest
class CardClientTest {

    @MockBean
    @Qualifier("schedulerRestTemplate")
    private RestTemplate restTemplate;

    @Value("${card.base-uri}")
    private String baseUri;

    @Autowired
    private CardClient cardClient;

    @Test
    void shouldCallCardServiceForNewCard() {
        CardResponse cardResponse = aCardResponse();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willReturn(new ResponseEntity<>(cardResponse, HttpStatus.CREATED));
        CardRequest cardRequest = aValidCardRequest();

        CardResponse response = cardClient.createNewCardRequest(cardRequest);

        verify(restTemplate).postForEntity(baseUri + "/v1/cards", cardRequest, CardResponse.class);
        assertThat(response.getCardAccountId()).isEqualTo(cardResponse.getCardAccountId());
    }

    @Test
    void shouldThrowExceptionWhenCardServiceReturnsUnexpectedStatusCodeForNewCard() {
        CardRequest cardRequest = aValidCardRequest();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        CardClientException exception = catchThrowableOfType(() -> cardClient.createNewCardRequest(cardRequest), CardClientException.class);

        assertThat(exception).as("Should throw an exception when response status is not CREATED").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Response code from card service was not CREATED, received: INTERNAL_SERVER_ERROR");
        verify(restTemplate).postForEntity(baseUri + "/v1/cards", cardRequest, CardResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenPostCallReturnsError() {
        CardRequest cardRequest = aValidCardRequest();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willThrow(new RestClientException("Test exception"));

        CardClientException exception = catchThrowableOfType(() -> cardClient.createNewCardRequest(cardRequest), CardClientException.class);

        assertThat(exception).as("Should throw an Exception when post call returns error").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Exception caught trying to call card service at: " + baseUri + "/v1/cards");
        verify(restTemplate).postForEntity(baseUri + "/v1/cards", cardRequest, CardResponse.class);
    }
}
