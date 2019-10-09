package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.scheduler.MessageProcessorScheduler;
import uk.gov.dhsc.htbhf.claimant.scheduler.PaymentCycleScheduler;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.dhsc.htbhf.claimant.testsupport.WiremockManager;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.EMAIL_DATE_PATTERN;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertThatPaymentCycleHasFailedPayments;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.formatVoucherAmount;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.getPaymentsWithStatus;
import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.*;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class PaymentCycleIntegrationTests {

    private static final LocalDate START_OF_NEXT_CYCLE = LocalDate.now().plusDays(28);
    private static final LocalDate TURNS_ONE_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(1).plusDays(4);
    private static final LocalDate TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(4).plusDays(4);
    private static final LocalDate SIX_MONTH_OLD = LocalDate.now().minusMonths(6);
    private static final LocalDate THREE_YEAR_OLD = LocalDate.now().minusYears(3);
    private static final List<LocalDate> SINGLE_THREE_YEAR_OLD = singletonList(THREE_YEAR_OLD);
    private static final int CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT = 88;
    private static final LocalDate DUE_DATE_IN_4_MONTHS = LocalDate.now().plusMonths(4);
    private static final LocalDate NOT_PREGNANT = null;

    @MockBean
    private NotificationClient notificationClient;
    private SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);

    @Autowired
    private PaymentCycleScheduler paymentCycleScheduler;
    @Autowired
    private MessageProcessorScheduler messageProcessorScheduler;
    @Autowired
    private RepositoryMediator repositoryMediator;
    @Autowired
    private WiremockManager wiremockManager;

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
    void shouldCreatePaymentCycleMakePaymentAndSendEmail() throws JsonProcessingException, NotificationClientException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();
        List<LocalDate> sixMonthOldAndThreeYearOld = asList(SIX_MONTH_OLD, THREE_YEAR_OLD);

        wiremockManager.stubSuccessfulEligibilityResponse(sixMonthOldAndThreeYearOld);
        wiremockManager.stubSuccessfulCardBalanceResponse(cardAccountId, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, sixMonthOldAndThreeYearOld, DUE_DATE_IN_4_MONTHS);

        invokeAllSchedulers();

        // confirm new payment cycle created with a payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedVoucherEntitlement = aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(
                LocalDate.now(), sixMonthOldAndThreeYearOld, claim.getClaimant().getExpectedDeliveryDate());
        assertPaymentCycleIsFullyPaid(newCycle, sixMonthOldAndThreeYearOld, expectedVoucherEntitlement);

        // confirm card service called to make payment
        Payment payment = newCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatPaymentEmailWasSent(newCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    void shouldSendEmailsWhenChildTurnsOneInNextPaymentCycle() throws JsonProcessingException, NotificationClientException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();
        List<LocalDate> childTurningOneInFirstWeekOfNextPaymentCycle = singletonList(TURNS_ONE_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE);

        wiremockManager.stubSuccessfulEligibilityResponse(childTurningOneInFirstWeekOfNextPaymentCycle);
        wiremockManager.stubSuccessfulCardBalanceResponse(cardAccountId, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, childTurningOneInFirstWeekOfNextPaymentCycle, NOT_PREGNANT);

        invokeAllSchedulers();

        // confirm card service called to make payment
        PaymentCycle currentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        Payment payment = currentCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatPaymentEmailWasSent(currentCycle);
        assertThatChildTurnsOneEmailWasSent(currentCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    void shouldSendEmailsWhenChildTurnsFourInNextPaymentCycle() throws JsonProcessingException, NotificationClientException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();
        List<LocalDate> childTurningFourInFirstWeekOfNextPaymentCycle = asList(TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE, THREE_YEAR_OLD);

        wiremockManager.stubSuccessfulEligibilityResponse(childTurningFourInFirstWeekOfNextPaymentCycle);
        wiremockManager.stubSuccessfulCardBalanceResponse(cardAccountId, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, childTurningFourInFirstWeekOfNextPaymentCycle, NOT_PREGNANT);

        invokeAllSchedulers();

        // confirm card service called to make payment
        PaymentCycle currentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        Payment payment = currentCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatPaymentEmailWasSent(currentCycle);
        assertThatChildTurnsFourEmailWasSent(currentCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void shouldRecoverFromErrorsToMakePaymentAndSendEmail() throws JsonProcessingException, NotificationClientException {
        String cardAccountId = UUID.randomUUID().toString();
        List<LocalDate> sixMonthOldAndThreeYearOld = asList(SIX_MONTH_OLD, THREE_YEAR_OLD);

        // all external endpoints will cause an error
        wiremockManager.stubErrorEligibilityResponse();
        wiremockManager.stubErrorCardBalanceResponse(cardAccountId);
        wiremockManager.stubErrorDepositResponse(cardAccountId);
        stubNotificationEmailError();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, sixMonthOldAndThreeYearOld, DUE_DATE_IN_4_MONTHS);

        // invoke all schedulers multiple times, fixing the next error in turn each time
        invokeAllSchedulers();
        wiremockManager.stubSuccessfulEligibilityResponse(sixMonthOldAndThreeYearOld);
        invokeAllSchedulers();
        wiremockManager.stubSuccessfulCardBalanceResponse(cardAccountId, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        invokeAllSchedulers();
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        invokeAllSchedulers();
        Mockito.reset(notificationClient); // necessary to clear the error and the count of attempts to send an email
        stubNotificationEmailResponse();
        invokeAllSchedulers();

        // confirm each error was recovered from, and the payment made successfully
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedVoucherEntitlement = aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(
                LocalDate.now(), sixMonthOldAndThreeYearOld, DUE_DATE_IN_4_MONTHS);
        assertPaymentCycleIsFullyPaid(newCycle, sixMonthOldAndThreeYearOld, expectedVoucherEntitlement);
        assertThatPaymentCycleHasFailedPayments(newCycle, 2);

        Payment payment = getPaymentsWithStatus(newCycle, PaymentStatus.SUCCESS).iterator().next();
        wiremockManager.assertThatGetBalanceRequestMadeForClaim(payment.getCardAccountId());
        wiremockManager.assertThatDepositFundsRequestMadeForPayment(payment);

        assertThatPaymentEmailWasSent(newCycle);
    }

    @Test
    void shouldSendNewChildEmailWhenPaymentCycleIncludesBackdatedVouchers() throws JsonProcessingException, NotificationClientException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();
        List<LocalDate> sixWeekOldAndThreeYearOld = asList(LocalDate.now().minusWeeks(6), THREE_YEAR_OLD);

        wiremockManager.stubSuccessfulEligibilityResponse(sixWeekOldAndThreeYearOld);
        wiremockManager.stubSuccessfulCardBalanceResponse(cardAccountId, CARD_BALANCE_IN_PENCE_BEFORE_DEPOSIT);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, SINGLE_THREE_YEAR_OLD, LocalDate.now().minusWeeks(7));

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

    @DisplayName("Integration test for HTBHF-2185 for a non-pregnant claimant where DWP has said they are eligible but have returned no children on record, "
            + "status should be set to Pending Expiry and email sent to Claimant")
    @Disabled("HTBHF-2185")
    @Test
    void shouldSendNoLongerEligibleEmailWhenEligibleWithNoChildrenOnFeedAndNotPregnant() throws JsonProcessingException, NotificationClientException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();

        List<LocalDate> currentPaymentCycleChildrenDobs = emptyList();
        List<LocalDate> previousPaymentCycleChildrenDobs = SINGLE_THREE_YEAR_OLD;
        wiremockManager.stubSuccessfulEligibilityResponse(currentPaymentCycleChildrenDobs);
        stubNotificationEmailResponse();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, previousPaymentCycleChildrenDobs, NOT_PREGNANT);

        invokeAllSchedulers();

        // confirm new payment cycle created with no payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        assertPaymentCycleWithNoPayment(newCycle, currentPaymentCycleChildrenDobs);

        assertStatusOnClaim(claim, ClaimStatus.PENDING_EXPIRY);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(cardAccountId);

        // confirm notify component invoked with correct email template & personalisation
        assertThatNoChildOnFeedNoLongerEligibleEmailWasSent(newCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-1757 status set to Pending Expiry and email sent to Claimant who has a child in current cycle, "
            + "pregnancy irrelevant, testing both with children in previous cycle and without")
    @ParameterizedTest(name = "Children DOB previous cycle={0}, Children DOB current cycle={1}, expected delivery date={2}")
    @MethodSource("provideChildrenDobForTest")
    void shouldTestClaimantBecomingIneligible(List<LocalDate> previousCycleChildrenDobs,
                                              List<LocalDate> currentCycleChildrenDobs,
                                              LocalDate expectedDeliveryDate) throws JsonProcessingException, NotificationClientException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();

        wiremockManager.stubIneligibleEligibilityResponse(currentCycleChildrenDobs);
        stubNotificationEmailResponse();

        //Create previous PaymentCycle
        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, previousCycleChildrenDobs, expectedDeliveryDate);

        invokeAllSchedulers();

        // confirm new payment cycle created with no payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        assertPaymentCycleWithNoPayment(newCycle, currentCycleChildrenDobs);

        assertStatusOnClaim(claim, ClaimStatus.PENDING_EXPIRY);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(cardAccountId);

        // confirm notify component invoked with correct email template & personalisation
        assertThatClaimNoLongerEligibleEmailWasSent(newCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    @DisplayName("Integration test for HTBHF-2182 status set to Expired and no email sent to Claimant who has no children in current cycle, "
            + "not pregnant, children over 4 present in previous cycle")
    @ParameterizedTest(name = "Eligibility status={0}")
    @ValueSource(strings = {"ELIGIBLE", "INELIGIBLE"})
    @Disabled("HTBHF-2182")
    void shouldTestClaimBecomingExpiredWhenRollingOffTheScheme(EligibilityStatus eligibilityStatus) throws JsonProcessingException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();

        LocalDate childTurnedFourInLastCycle = LocalDate.now().minusYears(4).minusWeeks(2);
        List<LocalDate> previousCycleChildrenDobs = singletonList(childTurnedFourInLastCycle);
        //DWP will not return children over 4 in response
        wiremockManager.stubEligibilityResponse(emptyList(), eligibilityStatus);

        //Create previous PaymentCycle
        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, previousCycleChildrenDobs, NOT_PREGNANT);

        invokeAllSchedulers();

        // confirm new payment cycle created with no payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        assertPaymentCycleWithNoPayment(newCycle, emptyList());

        assertStatusOnClaim(claim, ClaimStatus.EXPIRED);

        // confirm card service not called to make payment
        wiremockManager.assertThatDepositFundsRequestNotMadeForCard(cardAccountId);

        // confirm no emails sent to claimant
        verifyZeroInteractions(notificationClient);
    }

    //First argument is the previous cycle, second argument is the current cycle, third is the expected delivery date.
    private static Stream<Arguments> provideChildrenDobForTest() {
        return Stream.of(
                Arguments.of(SINGLE_THREE_YEAR_OLD, SINGLE_THREE_YEAR_OLD, DUE_DATE_IN_4_MONTHS),
                Arguments.of(emptyList(), SINGLE_THREE_YEAR_OLD, DUE_DATE_IN_4_MONTHS),
                Arguments.of(SINGLE_THREE_YEAR_OLD, emptyList(), DUE_DATE_IN_4_MONTHS),
                Arguments.of(emptyList(), emptyList(), DUE_DATE_IN_4_MONTHS),
                Arguments.of(SINGLE_THREE_YEAR_OLD, emptyList(), NOT_PREGNANT)
        );
    }

    private void assertStatusOnClaim(Claim claim, ClaimStatus expectedClaimStatus) {
        Claim updatedClaim = repositoryMediator.loadClaim(claim.getId());
        assertThat(updatedClaim.getClaimStatus()).isEqualTo(expectedClaimStatus);
    }

    private void stubNotificationEmailResponse() throws NotificationClientException {
        when(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenReturn(sendEmailResponse);
    }

    private void stubNotificationEmailError() throws NotificationClientException {
        when(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenThrow(new NotificationClientException("Something went wrong"));
    }

    private void invokeAllSchedulers() {
        paymentCycleScheduler.createNewPaymentCycles();
        messageProcessorScheduler.processDetermineEntitlementMessages();
        messageProcessorScheduler.processPaymentMessages();
        messageProcessorScheduler.processSendEmailMessages();
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

    private void assertPaymentCycleWithNoPayment(PaymentCycle paymentCycle, List<LocalDate> childrenDob) {
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(LocalDate.now());
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(LocalDate.now().plusDays(27));
        assertThat(paymentCycle.getChildrenDob()).isEqualTo(childrenDob);
        assertThat(paymentCycle.getVoucherEntitlement()).isNull();
        assertThat(paymentCycle.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.INELIGIBLE);
        assertThat(paymentCycle.getCardBalanceInPence()).isNull();
        assertThat(paymentCycle.getTotalEntitlementAmountInPence()).isNull();
        assertThat(paymentCycle.getPayments()).isEmpty();
    }

    private void assertThatChildTurnsOneEmailWasSent(PaymentCycle currentCycle) throws NotificationClientException {
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(CHILD_TURNS_ONE.getTemplateId()), eq(currentCycle.getClaim().getClaimant().getEmailAddress()), argumentCaptor.capture(), any(), any());
        assertChildTurnsOneEmailPersonalisationMap(currentCycle, argumentCaptor.getValue());
    }

    private void assertThatChildTurnsFourEmailWasSent(PaymentCycle currentCycle) throws NotificationClientException {
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(CHILD_TURNS_FOUR.getTemplateId()), eq(currentCycle.getClaim().getClaimant().getEmailAddress()), argumentCaptor.capture(), any(), any());
        assertChildTurnsFourEmailPersonalisationMap(currentCycle, argumentCaptor.getValue());
    }

    private void assertThatPaymentEmailWasSent(PaymentCycle newCycle) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(PAYMENT.getTemplateId()), eq(newCycle.getClaim().getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(), any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertPaymentEmailPersonalisationMap(newCycle, personalisationMap);
    }

    private void assertThatNewChildEmailWasSent(PaymentCycle newCycle) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(NEW_CHILD_FROM_PREGNANCY.getTemplateId()), eq(newCycle.getClaim().getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(),
                any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertPaymentEmailPersonalisationMap(newCycle, personalisationMap);
        assertThat(personalisationMap.get(BACKDATED_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(newCycle.getVoucherEntitlement().getBackdatedVouchers()));
    }

    private void assertThatNoChildOnFeedNoLongerEligibleEmailWasSent(PaymentCycle newCycle) throws NotificationClientException {
        assertThatNoLongerEligibleEmailWasSent(newCycle, NO_CHILD_ON_FEED_NO_LONGER_ELIGIBLE);
    }

    private void assertThatClaimNoLongerEligibleEmailWasSent(PaymentCycle newCycle) throws NotificationClientException {
        assertThatNoLongerEligibleEmailWasSent(newCycle, CLAIM_NO_LONGER_ELIGIBLE);
    }

    private void assertThatNoLongerEligibleEmailWasSent(PaymentCycle newCycle, EmailType emailType) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(emailType.getTemplateId()),
                eq(newCycle.getClaim().getClaimant().getEmailAddress()),
                mapArgumentCaptor.capture(),
                any(),
                any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertThat(personalisationMap).hasSize(2);
        assertNameEmailFields(newCycle, personalisationMap);
    }

    private void assertChildTurnsOneEmailPersonalisationMap(PaymentCycle currentCycle, Map personalisationMap) {
        assertCommonEmailFields(currentCycle, personalisationMap);
        assertThat(personalisationMap.get(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(0)); // child is turning one in the next payment cycle, so no vouchers going forward
        assertThat(personalisationMap.get(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // child is turning one in the next payment cycle, so one voucher per week going forward
        assertThat(personalisationMap.get(PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(5)); // two vouchers in first week plus one for weeks two, three and four
        assertThat(personalisationMap.get(REGULAR_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // child is turning one in the next payment cycle, so one voucher per week going forward
    }

    private void assertChildTurnsFourEmailPersonalisationMap(PaymentCycle currentCycle, Map personalisationMap) {
        assertCommonEmailFields(currentCycle, personalisationMap);
        assertThat(personalisationMap.get(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(0)); // no children under one
        assertThat(personalisationMap.get(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // only one child will be under four
        assertThat(personalisationMap.get(PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(5)); // four vouchers for the three year old and one voucher in first week only for child turning four
        assertThat(personalisationMap.get(REGULAR_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // child has turned four, so no vouchers going forward
    }

    private void assertPaymentEmailPersonalisationMap(PaymentCycle newCycle, Map personalisationMap) {
        PaymentCycleVoucherEntitlement entitlement = newCycle.getVoucherEntitlement();
        assertCommonEmailFields(newCycle, personalisationMap);
        assertThat(personalisationMap.get(PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(newCycle.getTotalVouchers()));
        assertThat(personalisationMap.get(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenUnderOne()));
        assertThat(personalisationMap.get(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenBetweenOneAndFour()));
    }

    private void assertCommonEmailFields(PaymentCycle newCycle, Map personalisationMap) {
        assertNameEmailFields(newCycle, personalisationMap);
        assertThat(personalisationMap.get(PREGNANCY_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(newCycle.getVoucherEntitlement().getVouchersForPregnancy()));
        assertThat(personalisationMap.get(NEXT_PAYMENT_DATE.getTemplateKeyName())).asString()
                .contains(newCycle.getCycleEndDate().plusDays(1).format(EMAIL_DATE_PATTERN));
    }

    private void assertNameEmailFields(PaymentCycle newCycle, Map personalisationMap) {
        Claimant claimant = newCycle.getClaim().getClaimant();
        assertThat(personalisationMap).isNotNull();
        assertThat(personalisationMap.get(FIRST_NAME.getTemplateKeyName())).isEqualTo(claimant.getFirstName());
        assertThat(personalisationMap.get(LAST_NAME.getTemplateKeyName())).isEqualTo(claimant.getLastName());
    }

    private Claim createClaimWithPaymentCycleEndingYesterday(String cardAccountId, List<LocalDate> childrensDatesOfBirth, LocalDate expectedDeliveryDate) {
        Claim claim = aValidClaimBuilder()
                .claimant(aClaimantWithExpectedDeliveryDate(expectedDeliveryDate))
                .cardAccountId(cardAccountId)
                .build();
        repositoryMediator.createAndSavePaymentCycle(claim, LocalDate.now().minusDays(28), childrensDatesOfBirth);
        return claim;
    }

}


