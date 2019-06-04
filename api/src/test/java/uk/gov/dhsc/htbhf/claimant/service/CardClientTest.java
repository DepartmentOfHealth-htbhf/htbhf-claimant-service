package uk.gov.dhsc.htbhf.claimant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.dhsc.htbhf.claimant.exception.CardClientException;
import uk.gov.dhsc.htbhf.claimant.model.card.CardBalanceResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardBalanceResponseTestDataFactory.aValidCardBalanceResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aValidCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.DepositFundsTestDataFactory.aValidDepositFundsRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.DepositFundsTestDataFactory.aValidDepositFundsResponse;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
@AutoConfigureWireMock(port = 8140)
class CardClientTest {

    private static final String CARD_ACCOUNT_ID = "myCardId";
    private static final String CARDS_URL = "/v1/cards";
    private static final String DEPOSIT_FUNDS_URL = CARDS_URL + "/" + CARD_ACCOUNT_ID + "/deposit";
    private static final String GET_BALANCE_URL = CARDS_URL + "/" + CARD_ACCOUNT_ID + "/balance";

    @Value("${card.services-base-uri}")
    private String baseUri;

    @Autowired
    private CardClient cardClient;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void shouldCallCardServiceForNewCard() throws JsonProcessingException {
        CardResponse cardResponse = aCardResponse();
        stubNewCardEndpointWithSuccessfulResponse(cardResponse);
        CardRequest cardRequest = aValidCardRequest();

        CardResponse response = cardClient.requestNewCard(cardRequest);

        verifyPostToNewCardEndpoint();
        assertThat(response.getCardAccountId()).isEqualTo(cardResponse.getCardAccountId());
    }

    @Test
    void shouldThrowExceptionWhenCardServiceReturnsUnexpectedStatusCodeForNewCard() {
        CardRequest cardRequest = aValidCardRequest();
        // return non-error status code to prevent rest template throwing an exception
        stubNewCardEndpointWithStatus(NO_CONTENT.value());

        CardClientException exception = catchThrowableOfType(() -> cardClient.requestNewCard(cardRequest), CardClientException.class);

        assertThat(exception).as("Should throw an exception when response status is not CREATED").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Response code from card service was not as expected, received: NO_CONTENT");
        verifyPostToNewCardEndpoint();
    }

    @Test
    void shouldThrowExceptionWhenNewCardServiceThrowsException() {
        CardRequest cardRequest = aValidCardRequest();
        // returning a 500 error will throw an exception in the rest template
        stubNewCardEndpointWithStatus(INTERNAL_SERVER_ERROR.value());

        CardClientException exception = catchThrowableOfType(() -> cardClient.requestNewCard(cardRequest), CardClientException.class);

        assertThat(exception).as("Should throw an exception when rest error occurs").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Exception caught trying to call card service at: " + baseUri + CARDS_URL);
        verifyPostToNewCardEndpoint();
    }

    @Test
    void shouldCallDepositFunds() throws JsonProcessingException {
        DepositFundsRequest request = aValidDepositFundsRequest();
        DepositFundsResponse expectedResponse = aValidDepositFundsResponse();
        stubDepositFundsEndpointWithSuccessfulResponse(expectedResponse);

        DepositFundsResponse response = cardClient.depositFundsToCard(CARD_ACCOUNT_ID, request);

        assertThat(response).isEqualTo(expectedResponse);
        verifyPostToDepositFundsEndpoint();
    }

    @Test
    void shouldThrowExceptionWhenDepositFundsReturnsUnexpectedResponse() {
        DepositFundsRequest request = aValidDepositFundsRequest();
        // return non-error status code to prevent rest template throwing an exception
        stubDepositFundsEndpointWithStatus(NO_CONTENT.value());

        CardClientException exception = catchThrowableOfType(() -> cardClient.depositFundsToCard(CARD_ACCOUNT_ID, request), CardClientException.class);

        assertThat(exception).as("Should throw an exception when response status is not OK").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Response code from card service was not as expected, received: NO_CONTENT");
        verifyPostToDepositFundsEndpoint();
    }

    @Test
    void shouldThrowExceptionWhenDepositFundsThrowsException() {
        DepositFundsRequest request = aValidDepositFundsRequest();
        // returning a 500 error will throw an exception in the rest template
        stubDepositFundsEndpointWithStatus(INTERNAL_SERVER_ERROR.value());

        CardClientException exception = catchThrowableOfType(() -> cardClient.depositFundsToCard(CARD_ACCOUNT_ID, request), CardClientException.class);

        assertThat(exception).as("Should throw an exception when rest error occurs").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Exception caught trying to call card service at: " + baseUri + DEPOSIT_FUNDS_URL);
        verifyPostToDepositFundsEndpoint();
    }

    @Test
    void shouldCallGetBalance() throws JsonProcessingException {
        CardBalanceResponse expectedResponse = aValidCardBalanceResponse();
        stubGetBalanceEndpointWithSuccessfulResponse(expectedResponse);

        CardBalanceResponse response = cardClient.getBalance(CARD_ACCOUNT_ID);

        assertThat(response).isEqualTo(expectedResponse);
        verifyGetToGetBalanceEndpoint();
    }

    @Test
    void shouldThrowExceptionWhenGetBalanceReturnsUnexpectedResponse() {
        // return non-error status code to prevent rest template throwing an exception
        stubGetBalanceEndpointWithStatus(NO_CONTENT.value());

        CardClientException exception = catchThrowableOfType(() -> cardClient.getBalance(CARD_ACCOUNT_ID), CardClientException.class);

        assertThat(exception).as("Should throw an exception when response status is not OK").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Response code from card service was not as expected, received: NO_CONTENT");
        verifyGetToGetBalanceEndpoint();
    }

    @Test
    void shouldThrowExceptionWhenGetBalanceThrowsException() {
        // returning a 500 error will throw an exception in the rest template
        stubGetBalanceEndpointWithStatus(INTERNAL_SERVER_ERROR.value());

        CardClientException exception = catchThrowableOfType(() -> cardClient.getBalance(CARD_ACCOUNT_ID), CardClientException.class);

        assertThat(exception).as("Should throw an exception when response status is not OK").isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Exception caught trying to call card service at: " + baseUri + GET_BALANCE_URL);
        verifyGetToGetBalanceEndpoint();
    }

    private void stubNewCardEndpointWithSuccessfulResponse(CardResponse cardResponse) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(cardResponse);
        stubFor(post(urlEqualTo(CARDS_URL)).willReturn(aResponse()
                .withStatus(CREATED.value())
                .withHeader("Content-Type", "application/json")
                .withBody(json)));
    }

    private void stubNewCardEndpointWithStatus(int status) {
        stubFor(post(urlEqualTo(CARDS_URL)).willReturn(aResponse().withStatus(status)));
    }

    private void stubDepositFundsEndpointWithSuccessfulResponse(DepositFundsResponse depositFundsResponse) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(depositFundsResponse);
        stubFor(post(urlEqualTo(DEPOSIT_FUNDS_URL)).willReturn(okJson(json)));
    }

    private void stubDepositFundsEndpointWithStatus(int status) {
        stubFor(post(urlEqualTo(DEPOSIT_FUNDS_URL)).willReturn(aResponse().withStatus(status)));
    }

    private void stubGetBalanceEndpointWithSuccessfulResponse(CardBalanceResponse cardBalanceResponse) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(cardBalanceResponse);
        stubFor(get(urlEqualTo(GET_BALANCE_URL)).willReturn(okJson(json)));
    }

    private void stubGetBalanceEndpointWithStatus(int status) {
        stubFor(get(urlEqualTo(GET_BALANCE_URL)).willReturn(aResponse().withStatus(status)));
    }

    private void verifyPostToNewCardEndpoint() {
        verify(exactly(1), postRequestedFor(urlEqualTo(CARDS_URL)));
    }

    private void verifyPostToDepositFundsEndpoint() {
        verify(exactly(1), postRequestedFor(urlEqualTo(DEPOSIT_FUNDS_URL)));
    }

    private void verifyGetToGetBalanceEndpoint() {
        verify(exactly(1), getRequestedFor(urlEqualTo(GET_BALANCE_URL)));
    }
}
