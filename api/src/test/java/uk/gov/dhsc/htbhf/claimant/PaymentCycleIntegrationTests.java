package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertThatPaymentCycleHasFailedPayments;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.getPaymentsWithStatus;
import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.PENDING_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.SCHEDULED_FOR_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.REGULAR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.RESTARTED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_ACTIVE_TO_EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_PENDING_EXPIRY_TO_EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.SCHEDULED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataResponseTestFactory.aPostcodeDataResponseObjectForPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_TOO_FAR_IN_PAST;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

class PaymentCycleIntegrationTests extends ScheduledServiceIntegrationTest {

    private static final LocalDate START_OF_NEXT_CYCLE = LocalDate.now().plusDays(28);
    private static final LocalDate TURNS_ONE_ON_FIRST_DAY_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(1);
    private static final LocalDate TURNS_ONE_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(1).plusDays(4);
    private static final LocalDate TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(4).plusDays(4);
    private static final LocalDate SIX_MONTH_OLD = LocalDate.now().minusMonths(6);
    private static final LocalDate THREE_YEAR_OLD = LocalDate.now().minusYears(3);
    private static final List<LocalDate> TWO_CHILDREN = asList(SIX_MONTH_OLD, THREE_YEAR_OLD);
    private static final List<LocalDate> SINGLE_THREE_YEAR_OLD = singletonList(THREE_YEAR_OLD);
    private static final LocalDate CHILD_TURNED_FOUR_IN_LAST_CYCLE = LocalDate.now().minusYears(4).minusWeeks(2);
    private static final List<LocalDate> SINGLE_CHILD_TURNED_FOUR_IN_LAST_CYCLE = singletonList(CHILD_TURNED_FOUR_IN_LAST_CYCLE);
    private static final int CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT = 88;
    private static final LocalDate NOT_PREGNANT = null;
    private static final String CARD_ACCOUNT_ID = UUID.randomUUID().toString();

    @ParameterizedTest(name = "Children DOB previous cycle={0}, children DOB current cycle={1}")
    @MethodSource("provideArgumentsForActiveClaimTests")
    void shouldCreatePaymentCycleMakePaymentAndSendEmailAndReportPayment(List<LocalDate> previousPaymentCycleChildrenDobs,
                                                                         List<LocalDate> currentPaymentCycleChildrenDobs)
            throws JsonProcessingException, NotificationClientException {
        testClaimIsActivePaymentMadeAndEmailSentAndPaymentReported(ACTIVE, REGULAR_PAYMENT,
                previousPaymentCycleChildrenDobs, currentPaymentCycleChildrenDobs);
    }

    @ParameterizedTest(name = "Children DOB previous cycle={0}, children DOB current cycle={1}")
    @MethodSource("provideArgumentsForActiveClaimTests")
    void shouldCreatePaymentCycleMakePaymentAndSendEmailForPendingExpiryStatus(List<LocalDate> previousPaymentCycleChildrenDobs,
                                                                               List<LocalDate> currentPaymentCycleChildrenDobs)
            throws JsonProcessingException, NotificationClientException {
        testClaimIsActivePaymentMadeAndEmailSentAndPaymentReported(PENDING_EXPIRY, RESTARTED_PAYMENT,
                previousPaymentCycleChildrenDobs, currentPaymentCycleChildrenDobs);
    }

    private void testClaimIsActivePaymentMadeAndEmailSentAndPaymentReported(ClaimStatus previousCycleClaimStatus,
                                                                            EmailType emailType,
                                                                            List<LocalDate> previousPaymentCycleChildrenDobs,
                                                                            List<LocalDate> currentPaymentCycleChildrenDobs)
            throws JsonProcessingException, NotificationClientException {

        wiremockManager.stubSuccessfulEligibilityResponse(currentPaymentCycleChildrenDobs);
        wiremockManager.stubSuccessfulCardBalanceResponse(CARD_ACCOUNT_ID, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(CARD_ACCOUNT_ID);
        stubNotificationEmailResponse();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(previousCycleClaimStatus,
                previousPaymentCycleChildrenDobs, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);

        String postcode = claim.getClaimant().getAddress().getPostcode();
        PostcodeDataResponse postcodeDataResponse = aPostcodeDataResponseObjectForPostcode(postcode);
        wiremockManager.stubPostcodeDataLookup(postcodeDataResponse);
        wiremockManager.stubGoogleAnalyticsCall();

        invokeAllSchedulers();
        // The processing of a make payment message will create a report payment message, so we need to invoke the schedulers again
        invokeAllSchedulers();

        // confirm new payment cycle created with a payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedVoucherEntitlement = aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(
                LocalDate.now(), currentPaymentCycleChildrenDobs, claim.getClaimant().getExpectedDeliveryDate());
        assertPaymentCycleIsFullyPaid(newCycle, currentPaymentCycleChildrenDobs, expectedVoucherEntitlement);

        assertClaimStatus(claim, ACTIVE);

        // confirm card service called to make payment
        Payment payment = newCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);
        wiremockManager.verifyPostcodesIoCalled(postcode);
        wiremockManager.verifyGoogleAnalyticsCalledForPaymentEvent(claim, SCHEDULED_PAYMENT, newCycle.getTotalEntitlementAmountInPence(),
                currentPaymentCycleChildrenDobs);

        // confirm notify component invoked with correct email template & personalisation
        assertThatPaymentEmailWasSent(newCycle, emailType);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    void shouldSendEmailsWhenChildTurnsOneInNextPaymentCycle() throws JsonProcessingException, NotificationClientException {
        List<LocalDate> childTurningOneInFirstWeekOfNextPaymentCycle = singletonList(TURNS_ONE_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE);

        wiremockManager.stubSuccessfulEligibilityResponse(childTurningOneInFirstWeekOfNextPaymentCycle);
        wiremockManager.stubSuccessfulCardBalanceResponse(CARD_ACCOUNT_ID, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(CARD_ACCOUNT_ID);
        stubNotificationEmailResponse();

        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(childTurningOneInFirstWeekOfNextPaymentCycle, NOT_PREGNANT);

        invokeAllSchedulers();

        // confirm card service called to make payment
        PaymentCycle currentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        Payment payment = currentCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatRegularPaymentEmailWasSent(currentCycle);
        assertThatChildTurnsOneInFirstWeekEmailWasSent(currentCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    void shouldSendEmailsWhenChildTurnsOneOnFirstDayOfNextPaymentCycle() throws JsonProcessingException, NotificationClientException {
        List<LocalDate> childTurningOneOnFirstDayOfNextPaymentCycle = singletonList(TURNS_ONE_ON_FIRST_DAY_OF_NEXT_PAYMENT_CYCLE);

        wiremockManager.stubSuccessfulEligibilityResponse(childTurningOneOnFirstDayOfNextPaymentCycle);
        wiremockManager.stubSuccessfulCardBalanceResponse(CARD_ACCOUNT_ID, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(CARD_ACCOUNT_ID);
        stubNotificationEmailResponse();

        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(childTurningOneOnFirstDayOfNextPaymentCycle, NOT_PREGNANT);

        invokeAllSchedulers();

        // confirm card service called to make payment
        PaymentCycle currentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        Payment payment = currentCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatRegularPaymentEmailWasSent(currentCycle);
        assertThatChildTurnsOneOnFirstDayEmailWasSent(currentCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    void shouldSendEmailsWhenChildTurnsFourInNextPaymentCycle() throws JsonProcessingException, NotificationClientException {
        List<LocalDate> childTurningFourInFirstWeekOfNextPaymentCycle = asList(TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE, THREE_YEAR_OLD);

        wiremockManager.stubSuccessfulEligibilityResponse(childTurningFourInFirstWeekOfNextPaymentCycle);
        wiremockManager.stubSuccessfulCardBalanceResponse(CARD_ACCOUNT_ID, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(CARD_ACCOUNT_ID);
        stubNotificationEmailResponse();

        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(childTurningFourInFirstWeekOfNextPaymentCycle, NOT_PREGNANT);

        invokeAllSchedulers();

        // confirm card service called to make payment
        PaymentCycle currentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        Payment payment = currentCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatRegularPaymentEmailWasSent(currentCycle);
        assertThatChildTurnsFourEmailWasSent(currentCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void shouldRecoverFromErrorsToMakePaymentAndSendEmail() throws JsonProcessingException, NotificationClientException {
        List<LocalDate> sixMonthOldAndThreeYearOld = asList(SIX_MONTH_OLD, THREE_YEAR_OLD);

        // all external endpoints will cause an error
        wiremockManager.stubErrorEligibilityResponse();
        wiremockManager.stubErrorCardBalanceResponse(CARD_ACCOUNT_ID);
        wiremockManager.stubErrorDepositResponse(CARD_ACCOUNT_ID);
        stubNotificationEmailError();

        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(sixMonthOldAndThreeYearOld, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);

        // invoke all schedulers multiple times, fixing the next error in turn each time
        invokeAllSchedulers();
        wiremockManager.stubSuccessfulEligibilityResponse(sixMonthOldAndThreeYearOld);
        invokeAllSchedulers();
        wiremockManager.stubSuccessfulCardBalanceResponse(CARD_ACCOUNT_ID, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        invokeAllSchedulers();
        wiremockManager.stubSuccessfulDepositResponse(CARD_ACCOUNT_ID);
        invokeAllSchedulers();
        Mockito.reset(notificationClient); // necessary to clear the error and the count of attempts to send an email
        stubNotificationEmailResponse();
        invokeAllSchedulers();

        // confirm each error was recovered from, and the payment made successfully
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedVoucherEntitlement = aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(
                LocalDate.now(), sixMonthOldAndThreeYearOld, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);
        assertPaymentCycleIsFullyPaid(newCycle, sixMonthOldAndThreeYearOld, expectedVoucherEntitlement);
        assertThatPaymentCycleHasFailedPayments(newCycle, 2);

        Payment payment = getPaymentsWithStatus(newCycle, PaymentStatus.SUCCESS).iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        assertThatRegularPaymentEmailWasSent(newCycle);
    }

    @Test
    void shouldSendNewChildEmailWhenPaymentCycleIncludesBackdatedVouchers() throws JsonProcessingException, NotificationClientException {
        List<LocalDate> sixWeekOldAndThreeYearOld = asList(LocalDate.now().minusWeeks(6), THREE_YEAR_OLD);

        wiremockManager.stubSuccessfulEligibilityResponse(sixWeekOldAndThreeYearOld);
        wiremockManager.stubSuccessfulCardBalanceResponse(CARD_ACCOUNT_ID, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(CARD_ACCOUNT_ID);
        stubNotificationEmailResponse();

        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(SINGLE_THREE_YEAR_OLD, LocalDate.now().minusWeeks(9));

        invokeAllSchedulers();

        // confirm new payment cycle created with a payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedVoucherEntitlement =
                aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild(LocalDate.now(), sixWeekOldAndThreeYearOld);
        assertPaymentCycleIsFullyPaid(newCycle, sixWeekOldAndThreeYearOld, expectedVoucherEntitlement);

        // confirm card service called to make payment
        Payment payment = newCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatNewChildEmailWasSent(newCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-2185 for a non-pregnant claimant where DWP have returned no children on record but they are ELIGIBLE, "
            + "the claim status should be set to Expired and email sent to Claimant")
    @Test
    void shouldSendNoLongerEligibleEmailWhenEligibleWithNoChildrenOnFeedAndNotPregnant()
            throws JsonProcessingException, NotificationClientException {
        List<LocalDate> currentPaymentCycleChildrenDobs = emptyList();
        List<LocalDate> previousPaymentCycleChildrenDobs = SINGLE_THREE_YEAR_OLD;
        wiremockManager.stubSuccessfulEligibilityResponse(currentPaymentCycleChildrenDobs);
        stubNotificationEmailResponse();

        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(previousPaymentCycleChildrenDobs, NOT_PREGNANT);

        invokeAllSchedulers();

        assertFullLengthPaymentCycleWithNoPayment(claim);

        assertClaimAndCardStatus(claim, EXPIRED, PENDING_CANCELLATION);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(CARD_ACCOUNT_ID);

        wiremockManager.verifyGoogleAnalyticsCalledForClaimEventWithNoChildren(claim, UPDATED_FROM_ACTIVE_TO_EXPIRED);

        // confirm notify component invoked with correct email template & personalisation
        assertThatNoChildOnFeedNoLongerEligibleEmailWasSent(claim);
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-2377 for a claimant with a due date in the past, but still receiving pregnancy vouchers due to the 12 week grace "
            + "period. On their penultimate payment cycle which they will receive pregnancy vouchers, the claimant is sent an email reminder about reporting"
            + "a birth.")
    @Test
    void shouldSendReportABirthReminderEmailWhenClaimantReceivesSecondToLastPregnancyVoucher() throws JsonProcessingException, NotificationClientException {
        // The claimant will receive pregnancy vouchers for this cycle and the one after but not after that (given a 12 week grace period and four week cycles).
        wiremockManager.stubSuccessfulEligibilityResponse(NO_CHILDREN);
        wiremockManager.stubSuccessfulCardBalanceResponse(CARD_ACCOUNT_ID, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(CARD_ACCOUNT_ID);
        stubNotificationEmailResponse();

        LocalDate expectedDueDate = LocalDate.now().minusWeeks(5);
        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(NO_CHILDREN, expectedDueDate);

        invokeAllSchedulers();

        // confirm new payment cycle created with a payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedVoucherEntitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();
        assertPaymentCycleIsFullyPaid(newCycle, emptyList(), expectedVoucherEntitlement);

        // confirm card service called to make payment
        Payment payment = newCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatRegularPaymentEmailWasSent(newCycle);
        assertThatReportABirthReminderEmailWasSent(claim);
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-1757 status set to Pending Expiry and email sent to Claimant to tell them they are no longer eligible")
    @ParameterizedTest(name = "Children DOB previous cycle={0}, expected delivery date={1}")
    @MethodSource("provideArgumentsForClaimantBecomingIneligibleTest")
    void shouldTestClaimantBecomingIneligible(List<LocalDate> previousCycleChildrenDobs,
                                              LocalDate expectedDeliveryDate) throws JsonProcessingException, NotificationClientException {
        wiremockManager.stubIneligibleEligibilityResponse();
        stubNotificationEmailResponse();

        //Create previous PaymentCycle
        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(previousCycleChildrenDobs, expectedDeliveryDate);

        invokeAllSchedulers();

        assertWeeklyPaymentCycleWithNoPayment(claim);

        assertClaimAndCardStatus(claim, PENDING_EXPIRY, PENDING_CANCELLATION);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(CARD_ACCOUNT_ID);

        wiremockManager.verifyGoogleAnalyticsCalledForClaimEventWithNoChildren(claim, UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY);

        // confirm notify component invoked with correct email template & personalisation
        assertThatClaimNoLongerEligibleEmailWasSent(claim);
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-2182 status set to Expired and no email sent to Claimant who has no children in current cycle, "
            + "not pregnant, children over 4 present in previous cycle, response from DWP has no children")
    @ParameterizedTest(name = "DWP eligibility status={0}")
    @ValueSource(strings = {"ELIGIBLE", "INELIGIBLE"})
    void shouldTestClaimBecomingExpiredWhenRollingOffTheScheme(EligibilityStatus eligibilityStatus) throws JsonProcessingException {
        List<LocalDate> currentPaymentCycleChildrenDobs = NO_CHILDREN;
        List<LocalDate> previousCycleChildrenDobs = SINGLE_CHILD_TURNED_FOUR_IN_LAST_CYCLE;
        //DWP will not return children over 4 in response
        wiremockManager.stubEligibilityResponse(currentPaymentCycleChildrenDobs, eligibilityStatus);

        //Create previous PaymentCycle
        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(previousCycleChildrenDobs, NOT_PREGNANT);

        invokeAllSchedulers();

        assertFullLengthPaymentCycleWithNoPayment(claim);

        assertClaimAndCardStatus(claim, EXPIRED, PENDING_CANCELLATION);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(CARD_ACCOUNT_ID);

        wiremockManager.verifyGoogleAnalyticsCalledForClaimEventWithNoChildren(claim, UPDATED_FROM_ACTIVE_TO_EXPIRED);

        // confirm no emails sent to claimant
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-2182 where a claimant that was pregnant in the previous cycle but is considered no longer pregnant in this cycle, "
            + "the claim status is set to Expired and no email is sent, DWP status is irrelevant.")
    @ParameterizedTest(name = "Eligibility status={0}, expected delivery date={1}")
    @MethodSource("provideArgumentsForTestingNoLongerPregnantWithNoChildren")
    void shouldTestClaimBecomingExpiredWhenNoLongerPregnantWithNoChildren(EligibilityStatus eligibilityStatus, LocalDate expectedDeliveryDateInCurrentCycle)
            throws JsonProcessingException {
        List<LocalDate> currentPaymentCycleChildrenDobs = NO_CHILDREN;
        List<LocalDate> previousCycleChildrenDobs = NO_CHILDREN;
        //DWP will not return children over 4 in response
        wiremockManager.stubEligibilityResponse(currentPaymentCycleChildrenDobs, eligibilityStatus);

        //Create previous PaymentCycle
        Claim claim = createActiveClaimWithPaymentCycleEndingYesterday(previousCycleChildrenDobs, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);

        //Update expected delivery date on the claim to reflect the change in delivery date
        claim.getClaimant().setExpectedDeliveryDate(expectedDeliveryDateInCurrentCycle);
        repositoryMediator.saveClaim(claim);

        invokeAllSchedulers();

        assertFullLengthPaymentCycleWithNoPayment(claim);

        assertClaimAndCardStatus(claim, EXPIRED, PENDING_CANCELLATION);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(CARD_ACCOUNT_ID);

        wiremockManager.verifyGoogleAnalyticsCalledForClaimEventWithNoChildren(claim, UPDATED_FROM_ACTIVE_TO_EXPIRED);

        // confirm no emails sent to claimant
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-1296 status set to Expired after 16 weeks Pending Expiry and email sent to say the claim has closed")
    @ParameterizedTest(name = "Children DOB previous cycle={0}, Children DOB current cycle={1}, expected delivery date={2}, eligibility status={3}")
    @MethodSource("provideArgumentsForTestingPendingExpiryClaim")
    void shouldTestClaimBecomingExpiredAfter16WeeksPendingExpiry(List<LocalDate> previousCycleChildrenDobs,
                                                                 List<LocalDate> currentCycleChildrenDobs,
                                                                 LocalDate expectedDeliveryDate,
                                                                 EligibilityStatus eligibilityStatus)
            throws JsonProcessingException, NotificationClientException {

        wiremockManager.stubEligibilityResponse(currentCycleChildrenDobs, eligibilityStatus);
        wiremockManager.stubGoogleAnalyticsCall();
        stubNotificationEmailResponse();

        // create previous PaymentCycle
        LocalDateTime claimStatusTimestamp = LocalDateTime.now().minusWeeks(17);
        LocalDateTime cardStatusTimestamp = LocalDateTime.now().minusWeeks(17);
        Claim claim = createClaimWithPaymentCycleEndingYesterday(PENDING_EXPIRY, PENDING_CANCELLATION, claimStatusTimestamp, cardStatusTimestamp,
                previousCycleChildrenDobs, expectedDeliveryDate);

        invokeAllSchedulers();

        assertFullLengthPaymentCycleWithNoPayment(claim);

        assertClaimAndCardStatus(claim, EXPIRED, SCHEDULED_FOR_CANCELLATION);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(CARD_ACCOUNT_ID);

        if (eligibilityStatus == ELIGIBLE) {
            wiremockManager.verifyGoogleAnalyticsCalledForClaimEvent(claim, UPDATED_FROM_PENDING_EXPIRY_TO_EXPIRED, currentCycleChildrenDobs);
        } else {
            // non eligible responses will not have children dates of birth
            wiremockManager.verifyGoogleAnalyticsCalledForClaimEventWithNoChildren(claim, UPDATED_FROM_PENDING_EXPIRY_TO_EXPIRED);
        }

        // confirm notify component invoked with correct email template & personalisation
        invokeAllSchedulers();
        assertThatCardIsAboutToBeCancelledEmailWasSent(claim);
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-1296 status remaining Pending Expiry if less than 16 weeks since becoming Pending Expiry. No emails are sent")
    @ParameterizedTest(name = "Children DOB previous cycle={0}, Children DOB current cycle={1}, expected delivery date={2}, eligibility status={3}")
    @MethodSource("provideArgumentsForTestingPendingExpiryClaim")
    void shouldTestClaimRemainingPendingExpiryIfLessThan16Weeks(List<LocalDate> previousCycleChildrenDobs,
                                                                List<LocalDate> currentCycleChildrenDobs,
                                                                LocalDate expectedDeliveryDate,
                                                                EligibilityStatus eligibilityStatus) throws JsonProcessingException {

        wiremockManager.stubEligibilityResponse(currentCycleChildrenDobs, eligibilityStatus);

        // create previous PaymentCycle
        LocalDateTime claimStatusTimestamp = LocalDateTime.now().minusWeeks(2);
        Claim claim = createClaimWithPaymentCycleEndingYesterday(PENDING_EXPIRY,
                claimStatusTimestamp,
                previousCycleChildrenDobs,
                expectedDeliveryDate);

        invokeAllSchedulers();

        assertWeeklyPaymentCycleWithNoPayment(claim);

        assertClaimStatus(claim, PENDING_EXPIRY);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(CARD_ACCOUNT_ID);

        // confirm notify component not invoked as no emails sent
        verifyNoMoreInteractions(notificationClient);
    }

    //First argument is the previous cycle, second is the expected delivery date.
    private static Stream<Arguments> provideArgumentsForClaimantBecomingIneligibleTest() {
        return Stream.of(
                Arguments.of(SINGLE_THREE_YEAR_OLD, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS),
                Arguments.of(NO_CHILDREN, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS),
                Arguments.of(SINGLE_THREE_YEAR_OLD, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS),
                Arguments.of(NO_CHILDREN, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS),
                Arguments.of(SINGLE_THREE_YEAR_OLD, NOT_PREGNANT)
        );
    }

    //First argument is the children's dobs in previous cycle, second argument is children's dobs in current cycle
    private static Stream<Arguments> provideArgumentsForActiveClaimTests() {
        return Stream.of(
                Arguments.of(TWO_CHILDREN, TWO_CHILDREN),
                Arguments.of(NO_CHILDREN, TWO_CHILDREN),
                Arguments.of(TWO_CHILDREN, NO_CHILDREN),
                Arguments.of(NO_CHILDREN, NO_CHILDREN)
        );
    }

    //First argument is the previous cycle, second argument is the current cycle, third is the expected delivery date, fourth is the eligibility status.
    private static Stream<Arguments> provideArgumentsForTestingPendingExpiryClaim() {
        return Stream.of(
                Arguments.of(SINGLE_THREE_YEAR_OLD, NO_CHILDREN, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, INELIGIBLE),
                Arguments.of(NO_CHILDREN, SINGLE_THREE_YEAR_OLD, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, INELIGIBLE),
                Arguments.of(NO_CHILDREN, NO_CHILDREN, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, INELIGIBLE),
                Arguments.of(SINGLE_THREE_YEAR_OLD, NO_CHILDREN, NOT_PREGNANT, ELIGIBLE),
                Arguments.of(SINGLE_THREE_YEAR_OLD, NO_CHILDREN, NOT_PREGNANT, INELIGIBLE),
                Arguments.of(SINGLE_CHILD_TURNED_FOUR_IN_LAST_CYCLE, NO_CHILDREN, NOT_PREGNANT, ELIGIBLE),
                Arguments.of(SINGLE_CHILD_TURNED_FOUR_IN_LAST_CYCLE, NO_CHILDREN, NOT_PREGNANT, INELIGIBLE)
        );
    }

    //First argument is the status returned by DWP, second argument is the expected delivery date of the claimant in the current cycle.
    private static Stream<Arguments> provideArgumentsForTestingNoLongerPregnantWithNoChildren() {
        return Stream.of(
                Arguments.of(INELIGIBLE, NOT_PREGNANT),
                Arguments.of(INELIGIBLE, EXPECTED_DELIVERY_DATE_TOO_FAR_IN_PAST),
                Arguments.of(ELIGIBLE, NOT_PREGNANT),
                Arguments.of(ELIGIBLE, EXPECTED_DELIVERY_DATE_TOO_FAR_IN_PAST)
        );
    }

    private void assertClaimStatus(Claim claim, ClaimStatus expectedClaimStatus) {
        Claim updatedClaim = repositoryMediator.loadClaim(claim.getId());
        assertThat(updatedClaim.getClaimStatus()).isEqualTo(expectedClaimStatus);
    }

    private void assertClaimAndCardStatus(Claim claim, ClaimStatus expectedClaimStatus, CardStatus expectedCardStatus) {
        Claim updatedClaim = repositoryMediator.loadClaim(claim.getId());
        assertThat(updatedClaim.getClaimStatus()).isEqualTo(expectedClaimStatus);
        assertThat(updatedClaim.getCardStatus()).isEqualTo(expectedCardStatus);
    }

    private void invokeAllSchedulers() {
        repositoryMediator.makeAllMessagesProcessable();
        paymentCycleScheduler.createNewPaymentCycles();
        messageProcessorScheduler.processDetermineEntitlementMessages();
        messageProcessorScheduler.processPaymentMessages();
        messageProcessorScheduler.processSendEmailMessages();
        messageProcessorScheduler.processReportPaymentMessages();
        messageProcessorScheduler.processReportClaimMessages();
        cardCancellationScheduler.handleCardsPendingCancellation();
    }

    private void assertPaymentCycleIsFullyPaid(PaymentCycle paymentCycle, List<LocalDate> childrensDatesOfBirth,
                                               PaymentCycleVoucherEntitlement expectedVoucherEntitlement) {
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(LocalDate.now());
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(LocalDate.now().plusDays(27));
        assertThat(paymentCycle.getChildrenDob()).isEqualTo(childrensDatesOfBirth);
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedVoucherEntitlement);
        assertThat(paymentCycle.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.FULL_PAYMENT_MADE);
        assertThat(paymentCycle.getCardBalanceInPence()).isEqualTo(CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        assertThat(paymentCycle.getTotalEntitlementAmountInPence()).isEqualTo(expectedVoucherEntitlement.getTotalVoucherValueInPence());
        assertThat(paymentCycle.getPayments()).isNotEmpty();
        List<Payment> successfulPayments = getPaymentsWithStatus(paymentCycle, PaymentStatus.SUCCESS);
        assertThat(successfulPayments).hasSize(1);
        Payment payment = successfulPayments.iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedVoucherEntitlement.getTotalVoucherValueInPence());
    }

    private void assertFullLengthPaymentCycleWithNoPayment(Claim claim) {
        assertPaymentCycleWithNoPayment(claim, LocalDate.now().plusDays(27));
    }

    private void assertWeeklyPaymentCycleWithNoPayment(Claim claim) {
        assertPaymentCycleWithNoPayment(claim, LocalDate.now().plusDays(6));
    }

    private void assertPaymentCycleWithNoPayment(Claim claim, LocalDate expectedEndDate) {
        PaymentCycle paymentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(LocalDate.now());
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(expectedEndDate);
        assertThat(paymentCycle.getChildrenDob()).isEmpty();
        assertThat(paymentCycle.getVoucherEntitlement()).isNull();
        assertThat(paymentCycle.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.INELIGIBLE);
        assertThat(paymentCycle.getCardBalanceInPence()).isNull();
        assertThat(paymentCycle.getTotalEntitlementAmountInPence()).isNull();
        assertThat(paymentCycle.getPayments()).isEmpty();
    }

    private Claim createActiveClaimWithPaymentCycleEndingYesterday(List<LocalDate> childrensDateOfBirth, LocalDate expectedDeliveryDate) {
        return createClaimWithPaymentCycleEndingYesterday(ACTIVE, LocalDateTime.now(), childrensDateOfBirth, expectedDeliveryDate);
    }

    private Claim createClaimWithPaymentCycleEndingYesterday(ClaimStatus claimStatus, List<LocalDate> childrensDateOfBirth, LocalDate expectedDeliveryDate) {
        return createClaimWithPaymentCycleEndingYesterday(claimStatus, LocalDateTime.now(), childrensDateOfBirth, expectedDeliveryDate);
    }

    private Claim createClaimWithPaymentCycleEndingYesterday(ClaimStatus claimStatus,
                                                             CardStatus cardStatus,
                                                             LocalDateTime claimStatusTimestamp,
                                                             LocalDateTime cardStatusTimestamp,
                                                             List<LocalDate> childrensDateOfBirth,
                                                             LocalDate expectedDeliveryDate) {
        Claim claim = aValidClaimBuilder()
                .claimant(aClaimantWithExpectedDeliveryDate(expectedDeliveryDate))
                .cardAccountId(CARD_ACCOUNT_ID)
                .claimStatus(claimStatus)
                .claimStatusTimestamp(claimStatusTimestamp)
                .cardStatus(cardStatus)
                .cardStatusTimestamp(cardStatusTimestamp)
                .build();

        repositoryMediator.createAndSavePaymentCycle(claim, LocalDate.now().minusDays(28), childrensDateOfBirth);
        return claim;
    }

    private Claim createClaimWithPaymentCycleEndingYesterday(ClaimStatus claimStatus,
                                                             LocalDateTime claimStatusTimestamp,
                                                             List<LocalDate> childrensDateOfBirth,
                                                             LocalDate expectedDeliveryDate) {
        Claim claim = aValidClaimBuilder()
                .claimant(aClaimantWithExpectedDeliveryDate(expectedDeliveryDate))
                .cardAccountId(CARD_ACCOUNT_ID)
                .claimStatus(claimStatus)
                .claimStatusTimestamp(claimStatusTimestamp)
                .build();

        repositoryMediator.createAndSavePaymentCycle(claim, LocalDate.now().minusDays(28), childrensDateOfBirth);
        return claim;
    }

}


