package uk.gov.dhsc.htbhf.claimant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.exception.CardClientException;
import uk.gov.dhsc.htbhf.claimant.model.card.*;

/**
 * Service for interacting with the card services api.
 */
@Service
@Slf4j
public class CardClient {

    private static final String CARDS_ENDPOINT = "/v1/cards";
    private final String cardsUri;
    private final RestTemplate restTemplate;

    /**
     * Uses a rest template which sets the session id to the current job id.
     *
     * @param baseUri      base url of the card services api.
     * @param restTemplate rest template which sets the session id to the current job id.
     */
    public CardClient(@Value("${card.services-base-uri}") String baseUri,
                      RestTemplate restTemplate) {
        this.cardsUri = baseUri + CARDS_ENDPOINT;
        this.restTemplate = restTemplate;
    }

    public CardResponse requestNewCard(CardRequest cardRequest) {
        try {
            ResponseEntity<CardResponse> response = restTemplate.postForEntity(cardsUri, cardRequest, CardResponse.class);
            checkResponse(response, HttpStatus.CREATED);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Exception caught trying to post to {}", cardsUri);
            throw new CardClientException(e, cardsUri);
        }
    }

    public DepositFundsResponse depositFundsToCard(String cardAccountId, DepositFundsRequest depositRequest) {
        String uri = String.format("%s/%s/deposit", cardsUri, cardAccountId);
        try {
            ResponseEntity<DepositFundsResponse> response = restTemplate.postForEntity(uri, depositRequest, DepositFundsResponse.class);
            checkResponse(response, HttpStatus.OK);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Exception caught trying to post to {}", uri);
            throw new CardClientException(e, uri);
        }
    }

    public CardBalanceResponse getBalance(String cardAccountId) {
        String uri = String.format("%s/%s/balance", cardsUri, cardAccountId);
        try {
            ResponseEntity<CardBalanceResponse> response = restTemplate.getForEntity(uri, CardBalanceResponse.class);
            checkResponse(response, HttpStatus.OK);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Exception caught trying to get the card balance at {}", uri);
            throw new CardClientException(e, uri);
        }
    }

    private void checkResponse(ResponseEntity<?> response, HttpStatus expectedStatus) {
        if (response.getStatusCode() != expectedStatus) {
            log.error("Expecting {} response from the card service api, instead received {} with response body {}",
                    expectedStatus, response.getStatusCode(), response.getBody());
            throw new CardClientException(response.getStatusCode());
        }
    }
}
