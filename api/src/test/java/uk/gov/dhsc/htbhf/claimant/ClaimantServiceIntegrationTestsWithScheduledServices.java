package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_AND_LISA_DOBS;
import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertThatPaymentCycleHasFailedPayments;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.buildCreateClaimRequestEntity;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.getPaymentsWithStatus;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.PENDING_DECISION;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aValidClaimDTOWitChildrenDob;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.MATCHED;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_HELD;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_MATCHED;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedAddressNotMatchedResponse;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithMatches;

public class ClaimantServiceIntegrationTestsWithScheduledServices extends ScheduledServiceIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldRequestNewCardAndSendEmailForSuccessfulClaim() throws JsonProcessingException, NotificationClientException {
        NewClaimDTO newClaimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = newClaimDTO.getClaimant();
        List<LocalDate> childrenDob = claimant.getInitiallyDeclaredChildrenDob();
        String cardAccountId = UUID.randomUUID().toString();
        wiremockManager.stubSuccessfulEligibilityResponse(childrenDob);
        wiremockManager.stubSuccessfulNewCardResponse(cardAccountId);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildCreateClaimRequestEntity(newClaimDTO), ClaimResultDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);

        invokeAllSchedulers();

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
    void shouldRequestNewCardAndSendTextMessageForSuccessfulClaim() throws JsonProcessingException, NotificationClientException {
        NewClaimDTO newClaimDTO = aValidClaimDTOWithNoNullFields();
        newClaimDTO.getClaimant().setEmailAddress(null);
        ClaimantDTO claimant = newClaimDTO.getClaimant();
        List<LocalDate> childrenDob = claimant.getInitiallyDeclaredChildrenDob();
        String cardAccountId = UUID.randomUUID().toString();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse
                = anIdMatchedEligibilityConfirmedUCResponseWithMatches(MATCHED, NOT_MATCHED, childrenDob);
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);
        wiremockManager.stubSuccessfulNewCardResponse(cardAccountId);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationTextResponse();

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildCreateClaimRequestEntity(newClaimDTO), ClaimResultDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);

        invokeAllSchedulers();

        Claim claim = repositoryMediator.getClaimForNino(claimant.getNino());
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        PaymentCycle paymentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedEntitlement =
                aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(LocalDate.now(), childrenDob, claim.getClaimant().getExpectedDeliveryDate());
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedEntitlement);
        assertThat(paymentCycle.getPayments()).isNotEmpty();
        Payment payment = paymentCycle.getPayments().iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedEntitlement.getTotalVoucherValueInPence());

        assertThatInstantSuccessTextSentCorrectly(claim, paymentCycle);
        wiremockManager.assertThatNewCardRequestMadeForClaim(claim);
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    void shouldSendWeWillLetYouKnowEmailAndLetterForAddressMismatch() throws JsonProcessingException, NotificationClientException {
        wiremockManager.stubEligibilityResponse(anIdMatchedEligibilityConfirmedAddressNotMatchedResponse());
        stubNotificationEmailResponse();
        stubNotificationLetterResponse();
        NewClaimDTO newClaimDTO = aValidClaimDTOWithNoNullFields();

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(newClaimDTO), ClaimResultDTO.class);
        invokeAllSchedulers();

        assertThat(response.getStatusCode()).isEqualTo(OK);
        Claim claim = repositoryMediator.getClaimForNino(newClaimDTO.getClaimant().getNino());
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);

        assertThatEmailWithNameOnlyWasSent(claim, PENDING_DECISION);
        assertThatLetterWithAddressOnlyWasSent(claim, LetterType.UPDATE_YOUR_ADDRESS);
        verifyNoMoreInteractions(notificationClient);
    }

    @ParameterizedTest
    @MethodSource("emailAndOrPhoneMismatchParameters")
    void shouldSendWeWillLetYouKnowEmailAndInstantSuccessLetterForEligibleApplicantWhoseEmailOrPhoneDontMatch(List<LocalDate> declaredChildrenDob,
                                                                                                              List<LocalDate> benefitAgencyChildrenDob,
                                                                                                              LetterType letterType)
            throws JsonProcessingException, NotificationClientException {
        wiremockManager.stubEligibilityResponse(anIdMatchedEligibilityConfirmedUCResponseWithMatches(NOT_MATCHED, NOT_HELD, benefitAgencyChildrenDob));
        String cardAccountId = UUID.randomUUID().toString();
        wiremockManager.stubSuccessfulNewCardResponse(cardAccountId);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();
        stubNotificationLetterResponse();
        NewClaimDTO newClaimDTO = aValidClaimDTOWitChildrenDob(declaredChildrenDob);

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(newClaimDTO), ClaimResultDTO.class);

        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        invokeAllSchedulers();
        Claim claim = repositoryMediator.getClaimForNino(newClaimDTO.getClaimant().getNino());
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        PaymentCycle paymentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedEntitlement = aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(
                LocalDate.now(), benefitAgencyChildrenDob, claim.getClaimant().getExpectedDeliveryDate());
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedEntitlement);
        assertThat(paymentCycle.getPayments()).isNotEmpty();
        Payment payment = paymentCycle.getPayments().iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedEntitlement.getTotalVoucherValueInPence());

        assertThatEmailWithNameOnlyWasSent(claim, PENDING_DECISION);
        assertThatLetterWithAddressAndPaymentFieldsWasSent(claim, paymentCycle, letterType);
        wiremockManager.assertThatNewCardRequestMadeForClaim(claim);
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);
        verifyNoMoreInteractions(notificationClient);
    }

    // send children_mismatch letter when declared children contains a dob not in the children returned from the benefit agency.
    private static Stream<Arguments> emailAndOrPhoneMismatchParameters() {
        return Stream.of(
                // only testing subset of all permutations of email/phone being matched, not_matched, not_held, not_set and not_supplied in this integration
                // test. A larger set of permutations is tested by a unit test.
                Arguments.of(MAGGIE_AND_LISA_DOBS, MAGGIE_AND_LISA_DOBS, LetterType.APPLICATION_SUCCESS_CHILDREN_MATCH),
                Arguments.of(MAGGIE_AND_LISA_DOBS, List.of(MAGGIE_DATE_OF_BIRTH), LetterType.APPLICATION_SUCCESS_CHILDREN_MISMATCH)
        );
    }

    @Test
    void shouldUpdateSuccessfulClaimWithPostcodeData() throws JsonProcessingException {
        NewClaimDTO newClaimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = newClaimDTO.getClaimant();
        String postcode = claimant.getAddress().getPostcode();
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(postcode);
        List<LocalDate> childrenDob = claimant.getInitiallyDeclaredChildrenDob();
        wiremockManager.stubSuccessfulEligibilityResponse(childrenDob);
        wiremockManager.stubSuccessfulPostcodesIoResponse(postcode, postcodeData);

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildCreateClaimRequestEntity(newClaimDTO), ClaimResultDTO.class);
        messageProcessorScheduler.processReportClaimMessages();

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        Claim claim = repositoryMediator.getClaimForNino(claimant.getNino());
        assertThat(claim.getPostcodeData()).isEqualTo(postcodeData);
        wiremockManager.assertThatPostcodeDataRetrievedForPostcode(postcode);
    }

    @Test
    void shouldUpdateSuccessfulClaimWhenPostcodesIOReturnsPostcodeNotFound() throws JsonProcessingException {
        NewClaimDTO newClaimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = newClaimDTO.getClaimant();
        String postcode = claimant.getAddress().getPostcode();
        List<LocalDate> childrenDob = claimant.getInitiallyDeclaredChildrenDob();
        wiremockManager.stubSuccessfulEligibilityResponse(childrenDob);
        wiremockManager.stubNotFoundPostcodesIOResponse(postcode);

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildCreateClaimRequestEntity(newClaimDTO), ClaimResultDTO.class);
        messageProcessorScheduler.processReportClaimMessages();

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        Claim claim = repositoryMediator.getClaimForNino(claimant.getNino());
        assertThat(claim.getPostcodeData()).isEqualTo(PostcodeData.NOT_FOUND);
        wiremockManager.assertThatPostcodeDataRetrievedForPostcode(postcode);
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void shouldRecoverFromErrorsToHandleSuccessfulClaim() throws JsonProcessingException, NotificationClientException {
        NewClaimDTO newClaimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimant = newClaimDTO.getClaimant();
        List<LocalDate> childrenDob = claimant.getInitiallyDeclaredChildrenDob();
        String cardAccountId = UUID.randomUUID().toString();

        wiremockManager.stubSuccessfulEligibilityResponse(childrenDob);
        // all external endpoints not invoked synchronously will cause an error
        wiremockManager.stubErrorNewCardResponse();
        wiremockManager.stubErrorDepositResponse(cardAccountId);
        String postcode = claimant.getAddress().getPostcode();
        wiremockManager.stubErrorPostcodesIoResponse(postcode);
        stubNotificationEmailError();

        ResponseEntity<ClaimResultDTO> response
                = restTemplate.exchange(buildCreateClaimRequestEntity(newClaimDTO), ClaimResultDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);

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
        messageProcessorScheduler.processRequestPaymentMessages();
        messageProcessorScheduler.processCompletePaymentMessages();
        messageProcessorScheduler.processSendEmailMessages();
        messageProcessorScheduler.processSendTextMessages();
        messageProcessorScheduler.processSendLetterMessages();
        messageProcessorScheduler.processReportClaimMessages();
    }

}
