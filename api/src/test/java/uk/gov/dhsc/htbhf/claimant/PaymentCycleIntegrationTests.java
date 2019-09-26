package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.scheduler.MessageProcessorScheduler;
import uk.gov.dhsc.htbhf.claimant.scheduler.PaymentCycleScheduler;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardBalanceResponseTestDataFactory.aValidCardBalanceResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.DepositFundsTestDataFactory.aValidDepositFundsResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.childrenWithBirthdates;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHER_VALUE_IN_PENCE;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class PaymentCycleIntegrationTests {

    private static final LocalDate SIX_MONTH_OLD = LocalDate.now().minusMonths(6);
    private static final LocalDate THREE_YEAR_OLD = LocalDate.now().minusYears(3);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);
    private static final DateTimeFormatter EMAIL_DATE_PATTERN = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static WireMockServer eligibilityServiceMock;
    private static WireMockServer cardServicesMock;

    private final DecimalFormat currencyFormat = new DecimalFormat("£#,#0.00");

    @MockBean
    private NotificationClient notificationClient;
    private SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);

    @Autowired
    private PaymentCycleScheduler paymentCycleScheduler;
    @Autowired
    private MessageProcessorScheduler messageProcessorScheduler;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private RepositoryMediator repositoryMediator;


    @BeforeAll
    static void setupWireMock() {
        eligibilityServiceMock = startWireMockServer(8100);
        cardServicesMock = startWireMockServer(8140);
    }

    @AfterAll
    static void stopWireMock() {
        eligibilityServiceMock.stop();
        cardServicesMock.stop();
    }

    @AfterEach
    void clearDatabase() {
        repositoryMediator.deleteAllEntities();
    }

    @Test
    void shouldCreatePaymentCycleMakePaymentAndSendEmail() throws JsonProcessingException, NotificationClientException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();
        List<LocalDate> sixMonthOldAndThreeYearOld = Arrays.asList(SIX_MONTH_OLD, THREE_YEAR_OLD);
        int cardBalanceInPenceBeforeDeposit = 88;

        stubEligibilityResponse(sixMonthOldAndThreeYearOld);
        stubCardServiceResponses(cardAccountId, cardBalanceInPenceBeforeDeposit);
        stubNotificationEmailResponse();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, sixMonthOldAndThreeYearOld, LocalDate.now().plusMonths(4));

        invokeAllSchedulers();

        // confirm new payment cycle created with a payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedVoucherEntitlement =
                aPaymentCycleVoucherEntitlement(LocalDate.now(), sixMonthOldAndThreeYearOld, claim.getClaimant().getExpectedDeliveryDate());
        assertPaymentCycleIsIsFullyPaid(newCycle, sixMonthOldAndThreeYearOld, cardBalanceInPenceBeforeDeposit, expectedVoucherEntitlement);

        // confirm card service called to make payment
        Payment payment = newCycle.getPayments().iterator().next();
        assertThatGetBalanceAndDepositFundsInvokedForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatPaymentEmailWasSent(newCycle);
    }

    private void stubEligibilityResponse(List<LocalDate> childrensDateOfBirth) throws JsonProcessingException {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithChildren(childrenWithBirthdates(childrensDateOfBirth));
        eligibilityServiceMock.stubFor(post(urlEqualTo("/v1/eligibility")).willReturn(jsonResponse(eligibilityResponse)));
    }

    private void stubCardServiceResponses(String cardAccountId, int cardBalanceInPenceBeforeDeposit) throws JsonProcessingException {
        cardServicesMock.stubFor(get(urlEqualTo("/v1/cards/" + cardAccountId + "/balance"))
                .willReturn(jsonResponse(aValidCardBalanceResponse(cardBalanceInPenceBeforeDeposit))));
        cardServicesMock.stubFor(post(urlEqualTo("/v1/cards/" + cardAccountId + "/deposit"))
                .willReturn(jsonResponse(aValidDepositFundsResponse())));
    }

    private void stubNotificationEmailResponse() throws NotificationClientException {
        when(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenReturn(sendEmailResponse);
    }

    private ResponseDefinitionBuilder jsonResponse(Object responseBody) throws JsonProcessingException {
        return okJson(objectMapper.writeValueAsString(responseBody));
    }

    private void invokeAllSchedulers() {
        paymentCycleScheduler.createNewPaymentCycles();
        messageProcessorScheduler.processDetermineEntitlementMessages();
        messageProcessorScheduler.processPaymentMessages();
        messageProcessorScheduler.processSendEmailMessages();
    }

    private void assertThatGetBalanceAndDepositFundsInvokedForPayment(Payment payment) throws JsonProcessingException {
        cardServicesMock.verify(getRequestedFor(urlEqualTo("/v1/cards/" + payment.getCardAccountId() + "/balance")));
        DepositFundsRequest expectedRequest = DepositFundsRequest.builder()
                .amountInPence(payment.getPaymentAmountInPence())
                .reference(payment.getId().toString())
                .build();
        StringValuePattern expectedBody = equalToJson(objectMapper.writeValueAsString(expectedRequest));
        cardServicesMock.verify(postRequestedFor(urlEqualTo("/v1/cards/" + payment.getCardAccountId() + "/deposit")).withRequestBody(expectedBody));
    }

    private void assertPaymentCycleIsIsFullyPaid(PaymentCycle paymentCycle, List<LocalDate> childrensDatesOfBirth,
                                                 int cardBalanceInPenceBeforeDeposit, PaymentCycleVoucherEntitlement expectedVoucherEntitlement) {
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(LocalDate.now());
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(LocalDate.now().plusDays(27));
        assertThat(paymentCycle.getChildrenDob()).isEqualTo(childrensDatesOfBirth);
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedVoucherEntitlement);
        assertThat(paymentCycle.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.FULL_PAYMENT_MADE);
        assertThat(paymentCycle.getCardBalanceInPence()).isEqualTo(cardBalanceInPenceBeforeDeposit);
        assertThat(paymentCycle.getTotalEntitlementAmountInPence()).isEqualTo(expectedVoucherEntitlement.getTotalVoucherValueInPence());
        assertThat(paymentCycle.getPayments()).hasSize(1);
        Payment payment = paymentCycle.getPayments().iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedVoucherEntitlement.getTotalVoucherValueInPence());

    }

    private void assertThatPaymentEmailWasSent(PaymentCycle newCycle) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(EmailType.PAYMENT.getTemplateId()), eq(newCycle.getClaim().getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(), any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertThat(personalisationMap).isNotNull();
        Claim claim = newCycle.getClaim();
        Claimant claimant = claim.getClaimant();
        PaymentCycleVoucherEntitlement entitlement = newCycle.getVoucherEntitlement();
        assertThat(personalisationMap.get(EmailTemplateKey.FIRST_NAME.getTemplateKeyName())).isEqualTo(claimant.getFirstName());
        assertThat(personalisationMap.get(EmailTemplateKey.LAST_NAME.getTemplateKeyName())).isEqualTo(claimant.getLastName());
        assertThat(personalisationMap.get(EmailTemplateKey.PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(newCycle.getTotalVouchers()));
        assertThat(personalisationMap.get(EmailTemplateKey.CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenUnderOne()));
        assertThat(personalisationMap.get(EmailTemplateKey.CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenBetweenOneAndFour()));
        assertThat(personalisationMap.get(EmailTemplateKey.PREGNANCY_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForPregnancy()));
        assertThat(personalisationMap.get(EmailTemplateKey.NEXT_PAYMENT_DATE.getTemplateKeyName())).asString()
                .contains(newCycle.getCycleEndDate().plusDays(1).format(EMAIL_DATE_PATTERN));
    }

    private String formatVoucherAmount(int voucherCount) {
        return currencyFormat.format(new BigDecimal(voucherCount * VOUCHER_VALUE_IN_PENCE).divide(ONE_HUNDRED));
    }

    private Claim createClaimWithPaymentCycleEndingYesterday(String cardAccountId, List<LocalDate> childrensDatesOfBirth, LocalDate expectedDeliveryDate) {
        Claim claim = aValidClaimBuilder()
                .claimant(aClaimantWithExpectedDeliveryDate(expectedDeliveryDate))
                .cardAccountId(cardAccountId)
                .build();
        repositoryMediator.createAndSavePaymentCycle(claim, LocalDate.now().minusDays(28), childrensDatesOfBirth);
        return claim;
    }

    private static WireMockServer startWireMockServer(int port) {
        WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(port));
        wireMockServer.start();
        return wireMockServer;
    }

}


