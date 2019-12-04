package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.eligibility.EligibilityDecisionHandler;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.TestConstants.ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimStatusAndClaimStatusTimestamp;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDateAndChildrenDobs;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aDetermineEntitlementMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aMessageWithTypeAndCreatedTimestamp;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartDateAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class DetermineEntitlementMessageProcessorTest {

    private static final Period MAXIMUM_PENDING_EXPIRY_DURATION = Period.ofWeeks(16);

    @Mock
    private EligibilityAndEntitlementService eligibilityAndEntitlementService;
    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private PaymentCycleService paymentCycleService;
    @Mock
    private EligibilityDecisionHandler eligibilityDecisionHandler;

    private DetermineEntitlementMessageProcessor processor;

    @BeforeEach
    void init() {
        processor = new DetermineEntitlementMessageProcessor(MAXIMUM_PENDING_EXPIRY_DURATION, eligibilityAndEntitlementService, messageContextLoader,
                paymentCycleService, eligibilityDecisionHandler);
    }

    @Test
    void shouldSuccessfullyProcessMessageAndTriggerPaymentWhenClaimantIsEligible() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext(
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR,
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility response with children returned
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateClaimantForPaymentCycle(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateClaimantForPaymentCycle(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verify(paymentCycleService).updatePaymentCycle(context.getCurrentPaymentCycle(), decision);
        verify(eligibilityDecisionHandler).handleEligibleDecision(context.getClaim(), context.getCurrentPaymentCycle());
    }

    @Test
    void shouldSendReportABirthReminderEmailWhenClaimantReceivesSecondToLastPregnancyVouchers() {
        //Given
        // The claimant will receive pregnancy vouchers for this cycle and the one after but not after that (given a 12 week grace period and four week cycles).
        LocalDate expectedDeliveryDate = LocalDate.now().minusWeeks(5);
        DetermineEntitlementMessageContext context = buildMessageContext(expectedDeliveryDate, NO_CHILDREN, expectedDeliveryDate, NO_CHILDREN);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility response with no children returned
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateClaimantForPaymentCycle(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(eligibilityDecisionHandler).handleEligibleDecision(context.getClaim(), context.getCurrentPaymentCycle());
    }

    @Test
    void shouldSuccessfullyProcessMessageAndHandleIneligibleDecision() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext(
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR,
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(INELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateClaimantForPaymentCycle(any(), any(), any())).willReturn(decision);
        doAnswer(invocation -> {
            Claim claim = invocation.getArgument(0);
            claim.updateClaimStatus(PENDING_EXPIRY);
            return null;
        }).when(eligibilityDecisionHandler).handleIneligibleDecisionForActiveClaim(any(), any(), any(), any());

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateClaimantForPaymentCycle(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verify(paymentCycleService).updatePaymentCycle(context.getCurrentPaymentCycle(), decision);
        verify(eligibilityDecisionHandler)
                .handleIneligibleDecisionForActiveClaim(context.getClaim(), context.getPreviousPaymentCycle(), context.getCurrentPaymentCycle(), decision);
        verify(paymentCycleService).updateEndDateForClaimBecomingPendingExpiry(context.getCurrentPaymentCycle());
    }

    @Test
    void shouldExpireClaimInPendingExpiryForExactlyMaximumPendingExpiryDuration() {
        LocalDateTime messageTimestamp = LocalDateTime.now();
        LocalDateTime claimStatusTimestamp = messageTimestamp.minus(MAXIMUM_PENDING_EXPIRY_DURATION);
        shouldExpireClaimInPendingExpiry(messageTimestamp, claimStatusTimestamp);
    }

    @Test
    void shouldExpireClaimInPendingExpiryForLongerThanMaximumPendingExpiryDuration() {
        LocalDateTime messageTimestamp = LocalDateTime.now();
        LocalDateTime claimStatusTimestamp = messageTimestamp.minus(MAXIMUM_PENDING_EXPIRY_DURATION).minusDays(1);
        shouldExpireClaimInPendingExpiry(messageTimestamp, claimStatusTimestamp);
    }

    private void shouldExpireClaimInPendingExpiry(LocalDateTime messageTimestamp, LocalDateTime claimStatusTimestamp) {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContextWithClaimInPendingExpiry(claimStatusTimestamp);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(INELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateClaimantForPaymentCycle(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aMessageWithTypeAndCreatedTimestamp(DETERMINE_ENTITLEMENT, messageTimestamp);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateClaimantForPaymentCycle(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());
        verify(eligibilityDecisionHandler).expirePendingExpiryClaim(context.getClaim(), decision.getDateOfBirthOfChildren());
    }

    @Test
    void shouldNotUpdateClaimInPendingExpiryLessThanMaximumPendingExpiryDuration() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContextWithClaimInPendingExpiry(LocalDateTime.now().minusWeeks(15));
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(INELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateClaimantForPaymentCycle(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        assertThat(context.getClaim().getClaimStatus()).isEqualTo(PENDING_EXPIRY);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateClaimantForPaymentCycle(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());
    }

    private DetermineEntitlementMessageContext buildMessageContextWithClaimInPendingExpiry(LocalDateTime claimStatusTimestamp) {
        Claim claim = aClaimWithClaimStatusAndClaimStatusTimestamp(PENDING_EXPIRY, claimStatusTimestamp);
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claim);
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claim);

        return aDetermineEntitlementMessageContext(currentPaymentCycle, previousPaymentCycle, claim);
    }

    private DetermineEntitlementMessageContext buildMessageContext(LocalDate previousCycleExpectedDeliveryDate,
                                                                   List<LocalDate> previousCycleChildrenDobs,
                                                                   LocalDate currentCycleExpectedDeliveryDate,
                                                                   List<LocalDate> currentCycleChildrenDobs) {
        //Claim - we are creating two claims to model the claim being at different states (differ by expected delivery date) at these points in time,
        //they are effectively the same Claim apart from the UUID being different (also on Claimant).
        Claim claimAtPreviousCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(previousCycleExpectedDeliveryDate, previousCycleChildrenDobs);
        Claim claimAtCurrentCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(currentCycleExpectedDeliveryDate, currentCycleChildrenDobs);

        //Current payment cycle
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claimAtCurrentCycle);

        //Previous payment cycle
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claimAtPreviousCycle);

        return aDetermineEntitlementMessageContext(
                currentPaymentCycle,
                previousPaymentCycle,
                claimAtCurrentCycle);
    }
}
