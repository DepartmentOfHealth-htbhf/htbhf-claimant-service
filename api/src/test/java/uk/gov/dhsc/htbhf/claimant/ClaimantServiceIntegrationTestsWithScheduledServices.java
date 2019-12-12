package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertThatPaymentCycleHasFailedPayments;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.buildClaimRequestEntityForUri;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.getPaymentsWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;

public class ClaimantServiceIntegrationTestsWithScheduledServices extends ScheduledServiceIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

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

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildClaimRequestEntityForUri(claimDTO, CLAIMANT_ENDPOINT_URI_V3), ClaimResultDTO.class);
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

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildClaimRequestEntityForUri(claimDTO, CLAIMANT_ENDPOINT_URI_V3), ClaimResultDTO.class);
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

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildClaimRequestEntityForUri(claimDTO, CLAIMANT_ENDPOINT_URI_V3), ClaimResultDTO.class);
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

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildClaimRequestEntityForUri(claimDTO, CLAIMANT_ENDPOINT_URI_V3), ClaimResultDTO.class);

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

    private void invokeAllSchedulers() {
        repositoryMediator.makeAllMessagesProcessable();
        messageProcessorScheduler.processRequestNewCardMessages();
        messageProcessorScheduler.processCompleteNewCardMessages();
        messageProcessorScheduler.processFirstPaymentMessages();
        messageProcessorScheduler.processSendEmailMessages();
        messageProcessorScheduler.processReportClaimMessages();
    }

}
