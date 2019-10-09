package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.model.*;
import uk.gov.dhsc.htbhf.claimant.scheduler.MessageProcessorScheduler;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.dhsc.htbhf.claimant.testsupport.WiremockManager;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CREATED;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class ClaimantServiceIntegrationTestsWithScheduledServices {

    @MockBean
    private NotificationClient notificationClient;
    private SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);

    @Autowired
    private MessageProcessorScheduler messageProcessorScheduler;
    @Autowired
    private RepositoryMediator repositoryMediator;
    @Autowired
    private WiremockManager wiremockManager;

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void setup() {
        wiremockManager.startWireMock();
    }

    @AfterEach
    void tearDown() {
        repositoryMediator.deleteAllEntities();
        wiremockManager.stopWireMock();
    }

    @Test
    void shouldRequestNewCardAndSendEmailForSuccessfulClaim() throws JsonProcessingException, NotificationClientException {
        ClaimDTO claimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = claimDTO.getClaimant();
        List<LocalDate> childrenDob = claimant.getChildrenDob();
        String cardAccountId = UUID.randomUUID().toString();
        wiremockManager.stubSuccessfulEligibilityResponse(childrenDob);
        wiremockManager.stubSuccessfulNewCardResponse(cardAccountId);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
        invokeAllSchedulers();

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        Claim claim = repositoryMediator.getClaimForNino(claimant.getNino());
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        PaymentCycle paymentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedEntitlement =
                aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(LocalDate.now(), childrenDob, claim.getClaimant().getExpectedDeliveryDate());
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedEntitlement);
        assertThat(paymentCycle.getPayments()).isNotEmpty();
        Payment payment = paymentCycle.getPayments().iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedEntitlement.getTotalVoucherValueInPence());

        assertThatNewCardEmailSentCorrectly(claim, paymentCycle);
        wiremockManager.assertThatNewCardRequestMadeForClaim(claim);
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);
    }

    @Test
    void shouldUpdateSuccessfulClaimWithPostcodeData() throws JsonProcessingException {
        ClaimDTO claimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = claimDTO.getClaimant();
        String postcode = claimant.getAddress().getPostcode();
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(postcode);
        List<LocalDate> childrenDob = claimant.getChildrenDob();
        wiremockManager.stubSuccessfulEligibilityResponse(childrenDob);
        wiremockManager.stubSuccessfulPostcodesIoResponse(postcode, postcodeData);

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
        messageProcessorScheduler.processReportClaimMessages();

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        Claim claim = repositoryMediator.getClaimForNino(claimant.getNino());
        assertThat(claim.getPostcodeData()).isEqualTo(postcodeData);
        wiremockManager.assertThatPostcodeDataRetrievedForPostcode(postcode);
    }

    @Test
    void shouldUpdateSuccessfulClaimWhenPostcodesIOReturnsPostcodeNotFound() throws JsonProcessingException {
        ClaimDTO claimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = claimDTO.getClaimant();
        String postcode = claimant.getAddress().getPostcode();
        List<LocalDate> childrenDob = claimant.getChildrenDob();
        wiremockManager.stubSuccessfulEligibilityResponse(childrenDob);
        wiremockManager.stubNotFoundPostcodesIOResponse(postcode);

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
        messageProcessorScheduler.processReportClaimMessages();

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        Claim claim = repositoryMediator.getClaimForNino(claimant.getNino());
        assertThat(claim.getPostcodeData()).isEqualTo(PostcodeData.NOT_FOUND);
        wiremockManager.assertThatPostcodeDataRetrievedForPostcode(postcode);
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void shouldRecoverFromErrorsToHandleSuccessfulClaim() throws JsonProcessingException, NotificationClientException {
        ClaimDTO claimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = claimDTO.getClaimant();
        List<LocalDate> childrenDob = claimant.getChildrenDob();
        String cardAccountId = UUID.randomUUID().toString();

        wiremockManager.stubSuccessfulEligibilityResponse(childrenDob);
        // all external endpoints not invoked synchronously will cause an error
        wiremockManager.stubErrorNewCardResponse();
        wiremockManager.stubErrorDepositResponse(cardAccountId);
        String postcode = claimant.getAddress().getPostcode();
        wiremockManager.stubErrorPostcodesIoResponse(postcode);
        stubNotificationEmailError();

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);

        // invoke all schedulers multiple times, fixing the next error in turn each time
        invokeAllSchedulers();
        wiremockManager.stubSuccessfulNewCardResponse(cardAccountId);
        invokeAllSchedulers();
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        invokeAllSchedulers();
        Mockito.reset(notificationClient); // necessary to clear the error and the count of attempts to send an email
        stubNotificationEmailResponse();
        invokeAllSchedulers();
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(postcode);
        wiremockManager.stubSuccessfulPostcodesIoResponse(postcode, postcodeData);
        invokeAllSchedulers();

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        Claim claim = repositoryMediator.getClaimForNino(claimant.getNino());
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        PaymentCycle paymentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedEntitlement =
                aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(LocalDate.now(), childrenDob, claim.getClaimant().getExpectedDeliveryDate());
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedEntitlement);
        List<Payment> successfulPayments = getPaymentsWithStatus(paymentCycle, PaymentStatus.SUCCESS);
        assertThat(successfulPayments).isNotEmpty();
        Payment payment = successfulPayments.iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedEntitlement.getTotalVoucherValueInPence());
        assertThatPaymentCycleHasFailedPayments(paymentCycle, 1);
        assertThat(claim.getPostcodeData()).isEqualTo(postcodeData);

        assertThatNewCardEmailSentCorrectly(claim, paymentCycle);
        wiremockManager.assertThatNewCardRequestMadeForClaim(claim);
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);
        wiremockManager.assertThatPostcodeDataRetrievedForPostcode(postcode);
    }

    private void assertThatNewCardEmailSentCorrectly(Claim claim, PaymentCycle paymentCycle) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(EmailType.NEW_CARD.getTemplateId()), eq(claim.getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(), any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertThat(personalisationMap).isNotNull();
        Claimant claimant = claim.getClaimant();
        PaymentCycleVoucherEntitlement entitlement = paymentCycle.getVoucherEntitlement();
        assertThat(personalisationMap.get(EmailTemplateKey.FIRST_NAME.getTemplateKeyName())).isEqualTo(claimant.getFirstName());
        assertThat(personalisationMap.get(EmailTemplateKey.LAST_NAME.getTemplateKeyName())).isEqualTo(claimant.getLastName());
        assertThat(personalisationMap.get(EmailTemplateKey.PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(paymentCycle.getTotalVouchers()));
        assertThat(personalisationMap.get(EmailTemplateKey.CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenUnderOne()));
        assertThat(personalisationMap.get(EmailTemplateKey.CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenBetweenOneAndFour()));
        assertThat(personalisationMap.get(EmailTemplateKey.PREGNANCY_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForPregnancy()));
        assertThat(personalisationMap.get(EmailTemplateKey.NEXT_PAYMENT_DATE.getTemplateKeyName())).asString()
                .contains(paymentCycle.getCycleEndDate().plusDays(1).format(EMAIL_DATE_PATTERN));
    }

    private void stubNotificationEmailResponse() throws NotificationClientException {
        when(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenReturn(sendEmailResponse);
    }

    private void stubNotificationEmailError() throws NotificationClientException {
        when(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenThrow(new NotificationClientException("Something went wrong"));
    }

    private void invokeAllSchedulers() {
        messageProcessorScheduler.processCreateNewCardMessages();
        messageProcessorScheduler.processFirstPaymentMessages();
        messageProcessorScheduler.processSendEmailMessages();
        messageProcessorScheduler.processReportClaimMessages();
    }

}
