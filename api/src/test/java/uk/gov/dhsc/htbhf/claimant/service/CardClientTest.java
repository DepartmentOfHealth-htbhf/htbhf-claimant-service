package uk.gov.dhsc.htbhf.claimant.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.exception.CardClientException;
import uk.gov.dhsc.htbhf.claimant.model.card.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardBalanceResponseTestDataFactory.aValidCardBalanceResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aValidCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.DepositFundsTestDataFactory.aValidDepositFundsRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.DepositFundsTestDataFactory.aValidDepositFundsResponse;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class CardClientTest {

    @MockBean
    private RestTemplate restTemplate;

    @Value("${card.services-base-uri}")
    private String baseUri;

    @Autowired
    private CardClient cardClient;
    public static final String CARD_ACCOUNT_ID = "myCardId";

    @Test
    void shouldCallCardServiceForNewCard() {
        CardResponse cardResponse = aCardResponse();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willReturn(new ResponseEntity<>(cardResponse, HttpStatus.CREATED));
        CardRequest cardRequest = aValidCardRequest();

        CardResponse response = cardClient.requestNewCard(cardRequest);

        verify(restTemplate).postForEntity(baseUri + "/v1/cards", cardRequest, CardResponse.class);
        assertThat(response.getCardAccountId()).isEqualTo(cardResponse.getCardAccountId());
    }

    @Test
    void shouldThrowExceptionWhenCardServiceReturnsUnexpectedStatusCodeForNewCard() {
        CardRequest cardRequest = aValidCardRequest();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        CardClientException exception = catchThrowableOfType(() -> cardClient.requestNewCard(cardRequest), CardClientException.class);

        assertThat(exception).as("Should throw an exception when response status is not CREATED").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Response code from card service was not as expected, received: INTERNAL_SERVER_ERROR");
        verify(restTemplate).postForEntity(baseUri + "/v1/cards", cardRequest, CardResponse.class);
    }

    @Test
    void shouldThrowAnExceptionWhenNewCardReturnsError() {
        CardRequest cardRequest = aValidCardRequest();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willThrow(new RestClientException("Test exception"));

        CardClientException exception = catchThrowableOfType(() -> cardClient.requestNewCard(cardRequest), CardClientException.class);

        assertThat(exception).as("Should throw an Exception when post call returns error").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Exception caught trying to call card service at: " + baseUri + "/v1/cards");
        verify(restTemplate).postForEntity(baseUri + "/v1/cards", cardRequest, CardResponse.class);
    }

    @Test
    void shouldCallDepositFunds() {
        DepositFundsRequest request = aValidDepositFundsRequest();
        DepositFundsResponse expectedResponse = aValidDepositFundsResponse();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        DepositFundsResponse response = cardClient.depositFundsToCard(CARD_ACCOUNT_ID, request);

        assertThat(response).isEqualTo(expectedResponse);
        verify(restTemplate).postForEntity(baseUri + "/v1/cards/" + CARD_ACCOUNT_ID + "/deposit", request, DepositFundsResponse.class);
    }

    @Test
    void shouldThrowExceptionWhenDepositFundsReturnsUnexpectedResponse() {
        DepositFundsRequest request = aValidDepositFundsRequest();
        DepositFundsResponse expectedResponse = aValidDepositFundsResponse();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willReturn(new ResponseEntity<>(expectedResponse, HttpStatus.BAD_REQUEST));

        CardClientException exception = catchThrowableOfType(() -> cardClient.depositFundsToCard(CARD_ACCOUNT_ID, request), CardClientException.class);

        assertThat(exception).as("Should throw an exception when response status is not OK").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Response code from card service was not as expected, received: BAD_REQUEST");
        verify(restTemplate).postForEntity(baseUri + "/v1/cards/" + CARD_ACCOUNT_ID + "/deposit", request, DepositFundsResponse.class);
    }

    @Test
    void shouldThrowExceptionWhenDepositFundsThrowsException() {
        DepositFundsRequest request = aValidDepositFundsRequest();
        given(restTemplate.postForEntity(anyString(), any(), any()))
                .willThrow(new RestClientException("Test exception"));

        CardClientException exception = catchThrowableOfType(() -> cardClient.depositFundsToCard(CARD_ACCOUNT_ID, request), CardClientException.class);

        assertThat(exception).as("Should throw an exception when rest error occurs").isNotNull();
        assertThat(exception.getMessage())
                .isEqualTo("Exception caught trying to call card service at: " + baseUri + "/v1/cards/" + CARD_ACCOUNT_ID + "/deposit");
        verify(restTemplate).postForEntity(baseUri + "/v1/cards/" + CARD_ACCOUNT_ID + "/deposit", request, DepositFundsResponse.class);
    }

    @Test
    void shouldCallGetBalance() {
        CardBalanceResponse expectedResponse = aValidCardBalanceResponse();
        given(restTemplate.getForEntity(anyString(), any())).willReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        CardBalanceResponse response = cardClient.getBalance(CARD_ACCOUNT_ID);

        assertThat(response).isEqualTo(expectedResponse);
        verify(restTemplate).getForEntity(baseUri + "/v1/cards/" + CARD_ACCOUNT_ID + "/balance", CardBalanceResponse.class);
    }

    @Test
    void shouldThrowExceptionWhenGetBalanceReturnsUnexpectedResponse() {
        CardBalanceResponse expectedResponse = aValidCardBalanceResponse();
        given(restTemplate.getForEntity(anyString(), any())).willReturn(new ResponseEntity<>(expectedResponse, HttpStatus.INTERNAL_SERVER_ERROR));

        CardClientException exception = catchThrowableOfType(() -> cardClient.getBalance(CARD_ACCOUNT_ID), CardClientException.class);

        assertThat(exception).as("Should throw an exception when response status is not OK").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Response code from card service was not as expected, received: INTERNAL_SERVER_ERROR");
        verify(restTemplate).getForEntity(baseUri + "/v1/cards/" + CARD_ACCOUNT_ID + "/balance", CardBalanceResponse.class);
    }

    @Test
    void shouldThrowExceptionWhenGetBalanceThrowsException() {
        given(restTemplate.getForEntity(anyString(), any())).willThrow(new RestClientException("Test exception"));

        CardClientException exception = catchThrowableOfType(() -> cardClient.getBalance(CARD_ACCOUNT_ID), CardClientException.class);

        assertThat(exception).as("Should throw an exception when rest error occurs").isNotNull();
        assertThat(exception.getMessage())
                .isEqualTo("Exception caught trying to call card service at: " + baseUri + "/v1/cards/" + CARD_ACCOUNT_ID + "/balance");
        verify(restTemplate).getForEntity(baseUri + "/v1/cards/" + CARD_ACCOUNT_ID + "/balance", CardBalanceResponse.class);
    }
}
