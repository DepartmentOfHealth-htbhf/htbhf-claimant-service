package uk.gov.dhsc.htbhf.claimant.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardBalanceResponseTestDataFactory.aValidCardBalanceResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.DepositFundsTestDataFactory.aValidDepositFundsResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.childrenWithBirthdates;

@Component
public class WiremockManager {
    private WireMockServer eligibilityServiceMock;
    private WireMockServer cardServiceMock;

    @Autowired
    private ObjectMapper objectMapper;

    public void startWireMock() {
        eligibilityServiceMock = startWireMockServer(8100);
        cardServiceMock = startWireMockServer(8140);
    }

    public void stopWireMock() {
        eligibilityServiceMock.stop();
        cardServiceMock.stop();
    }

    public void stubSuccessfulEligibilityResponse(List<LocalDate> childrensDateOfBirth) throws JsonProcessingException {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithChildren(childrenWithBirthdates(childrensDateOfBirth));
        eligibilityServiceMock.stubFor(post(urlEqualTo("/v1/eligibility")).willReturn(jsonResponse(eligibilityResponse)));
    }

    public void stubCardBalanceAndDepositResponses(String cardAccountId, int cardBalanceInPenceBeforeDeposit) throws JsonProcessingException {
        cardServiceMock.stubFor(get(urlEqualTo("/v1/cards/" + cardAccountId + "/balance"))
                .willReturn(jsonResponse(aValidCardBalanceResponse(cardBalanceInPenceBeforeDeposit))));
        cardServiceMock.stubFor(post(urlEqualTo("/v1/cards/" + cardAccountId + "/deposit"))
                .willReturn(jsonResponse(aValidDepositFundsResponse())));
    }

    public void stubNewCardAndDepositResponses(ClaimantDTO claimant, String cardAccountId) throws JsonProcessingException {
        CardRequest expectedRequest = expectedCardRequest(claimant);
        cardServiceMock.stubFor(post(urlEqualTo("/v1/cards"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(expectedRequest), true, true))
                .willReturn(jsonResponse(aCardResponse(cardAccountId))));
        cardServiceMock.stubFor(post(urlEqualTo("/v1/cards/" + cardAccountId + "/deposit"))
                .willReturn(jsonResponse(aValidDepositFundsResponse())));
    }

    public void assertThatGetBalanceAndDepositFundsInvokedForPayment(Payment payment) throws JsonProcessingException {
        cardServiceMock.verify(getRequestedFor(urlEqualTo("/v1/cards/" + payment.getCardAccountId() + "/balance")));
        StringValuePattern expectedBody = expectedDepositRequestBody(payment);
        cardServiceMock.verify(postRequestedFor(urlEqualTo("/v1/cards/" + payment.getCardAccountId() + "/deposit")).withRequestBody(expectedBody));
    }

    public void assertThatNewCardAndDepositFundsInvokedForClaim(Claim claim, Payment payment) throws JsonProcessingException {
        StringValuePattern expectedCardRequestBody = equalToJson(objectMapper.writeValueAsString(aCardRequest(claim)));
        cardServiceMock.verify(postRequestedFor(urlEqualTo("/v1/cards")).withRequestBody(expectedCardRequestBody));
        StringValuePattern expectedDepositBody = expectedDepositRequestBody(payment);
        cardServiceMock.verify(postRequestedFor(urlEqualTo("/v1/cards/" + payment.getCardAccountId() + "/deposit"))
                .withRequestBody(expectedDepositBody));
    }

    private CardRequest expectedCardRequest(ClaimantDTO claimant) {
        AddressDTO address = claimant.getAddress();
        return CardRequest.builder()
                .address(AddressDTO.builder()
                        .addressLine1(address.getAddressLine1())
                        .addressLine2(address.getAddressLine2())
                        .townOrCity(address.getTownOrCity())
                        .postcode(address.getPostcode())
                        .county(address.getCounty())
                        .build())
                .dateOfBirth(claimant.getDateOfBirth())
                .firstName(claimant.getFirstName())
                .lastName(claimant.getLastName())
                .email(claimant.getEmailAddress())
                .mobile(claimant.getPhoneNumber())
                .build();
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
