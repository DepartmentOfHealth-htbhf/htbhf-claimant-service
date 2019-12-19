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
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;
import uk.gov.dhsc.htbhf.claimant.model.*;
import uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertThatPaymentCycleHasFailedPayments;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.buildClaimRequestEntity;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.getPaymentsWithStatus;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.PENDING_DECISION;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedAddressNotMatchedResponse;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithMatches;

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
                = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
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

        assertThatInstantSuccessEmailSentCorrectly(claim, paymentCycle);
        wiremockManager.assertThatNewCardRequestMadeForClaim(claim);
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    void shouldSendWeWillLetYouKnowEmailAndLetterForAddressMismatch() throws JsonProcessingException, NotificationClientException {
        wiremockManager.stubEligibilityResponse(anIdMatchedEligibilityConfirmedAddressNotMatchedResponse());
        stubNotificationEmailResponse();
        stubNotificationLetterResponse();
        ClaimDTO claimDTO = aValidClaimDTOWithNoNullFields();

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
        invokeAllSchedulers();

        assertThat(response.getStatusCode()).isEqualTo(OK);
        Claim claim = repositoryMediator.getClaimForNino(claimDTO.getClaimant().getNino());
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);

        assertThatEmailWithNameOnlyWasSent(claim, PENDING_DECISION);
        assertThatLetterWithAddressOnlyWasSent(claim, LetterType.UPDATE_YOUR_ADDRESS);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    void shouldSendWeWillLetYouKnowEmailForEligibleApplicantWhoseEmailOrPhoneDontMatch() throws JsonProcessingException, NotificationClientException {
        ClaimDTO claimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = claimDTO.getClaimant();
        List<LocalDate> childrenDob = claimant.getChildrenDob();
        wiremockManager.stubEligibilityResponse(anIdMatchedEligibilityConfirmedUCResponseWithMatches(
                VerificationOutcome.NOT_MATCHED, VerificationOutcome.NOT_HELD, childrenDob));
        String cardAccountId = UUID.randomUUID().toString();
        wiremockManager.stubSuccessfulNewCardResponse(cardAccountId);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();
        stubNotificationLetterResponse();

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
        invokeAllSchedulers();

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

        assertThatEmailWithNameOnlyWasSent(claim, PENDING_DECISION);
        wiremockManager.assertThatNewCardRequestMadeForClaim(claim);
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);
        verifyNoMoreInteractions(notificationClient);
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
                = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
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
                = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
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
                = restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);

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

        assertThatInstantSuccessEmailSentCorrectly(claim, paymentCycle);
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
        messageProcessorScheduler.processSendLetterMessages();
        messageProcessorScheduler.processReportClaimMessages();
    }

}
