package uk.gov.dhsc.htbhf.claimant.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardBalanceResponseTestDataFactory.aValidCardBalanceResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.DepositFundsTestDataFactory.aValidDepositFundsResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithChildrenAndStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.childrenWithBirthdates;

@Component
public class WiremockManager {
    private static final String POSTCODES_IO_PATH = "/postcodes/";
    private static final String REPORT_ENDPOINT = "/collect";
    private static final String V1_ELIGIBILITY_URL = "/v1/eligibility";
    private static final String V1_CARDS_URL = "/v1/cards";
    private static final String POSTCODES_URL = "/postcodes/";
    private WireMockServer eligibilityServiceMock;
    private WireMockServer cardServiceMock;
    private WireMockServer postcodesMock;
    private WireMockServer googleAnalyticsMock;

    @Autowired
    private ObjectMapper objectMapper;

    public void startWireMock() {
        eligibilityServiceMock = startWireMockServer(8100);
        cardServiceMock = startWireMockServer(8140);
        postcodesMock = startWireMockServer(8120);
        googleAnalyticsMock = startWireMockServer(8150);
    }

    public void stopWireMock() {
        eligibilityServiceMock.stop();
        cardServiceMock.stop();
        postcodesMock.stop();
        googleAnalyticsMock.stop();
    }

    public void stubSuccessfulEligibilityResponse(List<LocalDate> childrensDateOfBirth) throws JsonProcessingException {
        stubEligibilityResponse(childrensDateOfBirth, EligibilityStatus.ELIGIBLE);
    }

    public void stubIneligibleEligibilityResponse(List<LocalDate> childrensDateOfBirth) throws JsonProcessingException {
        stubEligibilityResponse(childrensDateOfBirth, EligibilityStatus.INELIGIBLE);
    }

    public void stubEligibilityResponse(List<LocalDate> childrensDateOfBirth, EligibilityStatus eligibilityStatus) throws JsonProcessingException {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithChildrenAndStatus(childrenWithBirthdates(childrensDateOfBirth),
                eligibilityStatus);
        eligibilityServiceMock.stubFor(post(urlEqualTo(V1_ELIGIBILITY_URL)).willReturn(jsonResponse(eligibilityResponse)));
    }

    public void stubErrorEligibilityResponse() {
        eligibilityServiceMock.stubFor(post(urlEqualTo(V1_ELIGIBILITY_URL)).willReturn(aResponse()
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withBody("Something went badly wrong")));
    }

    public void stubSuccessfulDepositResponse(String cardAccountId) throws JsonProcessingException {
        cardServiceMock.stubFor(post(urlEqualTo("/v1/cards/" + cardAccountId + "/deposit"))
                .willReturn(jsonResponse(aValidDepositFundsResponse())));
    }

    public void stubErrorDepositResponse(String cardAccountId) {
        cardServiceMock.stubFor(post(urlEqualTo("/v1/cards/" + cardAccountId + "/deposit"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("Something went badly wrong")));
    }

    public void stubSuccessfulCardBalanceResponse(String cardAccountId, int cardBalanceInPenceBeforeDeposit) throws JsonProcessingException {
        cardServiceMock.stubFor(get(urlEqualTo("/v1/cards/" + cardAccountId + "/balance"))
                .willReturn(jsonResponse(aValidCardBalanceResponse(cardBalanceInPenceBeforeDeposit))));
    }

    public void stubErrorCardBalanceResponse(String cardAccountId) {
        cardServiceMock.stubFor(get(urlEqualTo("/v1/cards/" + cardAccountId + "/balance"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("Something went badly wrong")));
    }

    public void stubSuccessfulNewCardResponse(String cardAccountId) throws JsonProcessingException {
        cardServiceMock.stubFor(post(urlEqualTo(V1_CARDS_URL))
                .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(aCardResponse(cardAccountId)))
                ));
    }

    public void stubErrorNewCardResponse() {
        cardServiceMock.stubFor(post(urlEqualTo(V1_CARDS_URL))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("Something went badly wrong")));
    }

    public void stubSuccessfulPostcodesIoResponse(String postcode, PostcodeData postcodeData) throws JsonProcessingException {
        PostcodeDataResponse postcodeDataResponse = new PostcodeDataResponse(postcodeData);
        postcodesMock.stubFor(get(urlEqualTo(getPostcodeUrl(postcode)))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(postcodeDataResponse))));
    }

    public void stubNotFoundPostcodesIOResponse(String postcode) {
        postcodesMock.stubFor(get(urlEqualTo(getPostcodeUrl(postcode)))
                .willReturn(notFound()));
    }

    public void stubErrorPostcodesIoResponse(String postcode) {
        postcodesMock.stubFor(get(urlEqualTo(getPostcodeUrl(postcode)))
                .willReturn(serverError()));
    }

    public void stubPostcodeDataLookup(PostcodeDataResponse postcodeDataResponse) throws JsonProcessingException {
        String responseBody = objectMapper.writeValueAsString(postcodeDataResponse);
        String postcodeWithoutSpace = postcodeDataResponse.getPostcodeData().getPostcode().replace(" ", "");
        postcodesMock.stubFor(get(urlEqualTo(POSTCODES_IO_PATH + postcodeWithoutSpace))
                .willReturn(okJson(responseBody)));
    }

    public void stubGoogleAnalyticsCall() {
        googleAnalyticsMock.stubFor(post(urlEqualTo(REPORT_ENDPOINT)).withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .willReturn(ok()));
    }

    public void assertThatGetBalanceRequestMadeForClaim(String cardAccountId) {
        cardServiceMock.verify(getRequestedFor(urlEqualTo("/v1/cards/" + cardAccountId + "/balance")));
    }

    public void assertThatDepositFundsRequestMadeForPayment(Payment payment) throws JsonProcessingException {
        StringValuePattern expectedDepositBody = expectedDepositRequestBody(payment);
        cardServiceMock.verify(postRequestedFor(urlEqualTo("/v1/cards/" + payment.getCardAccountId() + "/deposit"))
                .withRequestBody(expectedDepositBody));
    }

    public void assertThatDepositFundsRequestNotMadeForCard(String cardAccountId) {
        cardServiceMock.verify(0, postRequestedFor(urlEqualTo("/v1/cards/" + cardAccountId + "/deposit")));
    }

    public void assertThatNewCardRequestMadeForClaim(Claim claim) throws JsonProcessingException {
        StringValuePattern expectedCardRequestBody = equalToJson(objectMapper.writeValueAsString(aCardRequest(claim)));
        cardServiceMock.verify(postRequestedFor(urlEqualTo(V1_CARDS_URL)).withRequestBody(expectedCardRequestBody));
    }

    public void assertThatPostcodeDataRetrievedForPostcode(String postcode) {
        postcodesMock.verify(getRequestedFor(urlEqualTo(getPostcodeUrl(postcode))));
    }

    public void verifyPostcodesIoCalled(String postcode) {
        String postcodeWithoutSpace = postcode.replace(" ", "");
        String expectedPostcodesUrl = POSTCODES_IO_PATH + postcodeWithoutSpace;
        postcodesMock.verify(1, getRequestedFor(urlEqualTo(expectedPostcodesUrl)));
    }

    public void verifyGoogleAnalyticsCalledForClaimEvent(Claim claim, ClaimAction claimAction, String trackingId) {
        // not asserting the full payload as it contains time based values and a large amount of data that would make the test fragile.
        // testing that the payload is created and sent correctly is covered by GoogleAnalyticsClientTest
        googleAnalyticsMock.verify(1, postRequestedFor(urlEqualTo(REPORT_ENDPOINT))
                .withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .withRequestBody(matching(
                        "t=event"
                                + "&v=1" // version 1
                                + "&tid=" + trackingId // tracking id from properties
                                + "&ec=CLAIM" // event category is CLAIM
                                + "&ea=" + claimAction.name()  // event action is the claim action (e.g. NEW, REJECTED, etc)
                                + "&ev=0" // event value is unused, so set to 0
                                + "&qt=\\d+" // queue time should be an integer
                                + "&cid=" + claim.getId() // customer id is the claim id
                                + ".*"))); // rest of payload data
    }

    public void verifyGoogleAnalyticsCalledForPaymentEvent(Claim claim, PaymentAction paymentAction, String trackingId, Integer paymentAmount) {
        // not asserting the full payload as it contains time based values and a large amount of data that would make the test fragile.
        // testing that the payload is created and sent correctly is covered by GoogleAnalyticsClientTest
        googleAnalyticsMock.verify(1, postRequestedFor(urlEqualTo(REPORT_ENDPOINT))
                .withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .withRequestBody(matching(
                        "t=event"
                                + "&v=1" // version 1
                                + "&tid=" + trackingId // tracking id from properties
                                + "&ec=PAYMENT" // event category is PAYMENT
                                + "&ea=" + paymentAction.name()  // event action is the payment action (e.g. INITIAL, SCHEDULED, etc)
                                + "&ev=" + paymentAmount.toString() // event value is the total payment amount
                                + "&qt=\\d+" // queue time should be an integer
                                + "&cid=" + claim.getId() // customer id is the claim id
                                + ".*"))); // rest of payload data
    }

    private String getPostcodeUrl(String postcode) {
        return POSTCODES_URL + postcode.toUpperCase().replace(" ", "");
    }

    private StringValuePattern expectedDepositRequestBody(Payment payment) throws JsonProcessingException {
        DepositFundsRequest expectedRequest = DepositFundsRequest.builder()
                .amountInPence(payment.getPaymentAmountInPence())
                .reference(payment.getId().toString())
                .build();
        return equalToJson(objectMapper.writeValueAsString(expectedRequest));
    }

    private ResponseDefinitionBuilder jsonResponse(Object responseBody) throws JsonProcessingException {
        return okJson(objectMapper.writeValueAsString(responseBody));
    }

    private static WireMockServer startWireMockServer(int port) {
        WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(port));
        wireMockServer.start();
        return wireMockServer;
    }

}
