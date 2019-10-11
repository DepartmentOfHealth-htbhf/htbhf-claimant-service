package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.communications.DetermineEntitlementNotificationHandler;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anIneligibleDecisionWithNoChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aDetermineEntitlementMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartDateAndClaim;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@ExtendWith(MockitoExtension.class)
class DetermineEntitlementMessageProcessorTest {

    private static final LocalDate EXPECTED_DELIVERY_DATE = LocalDate.now().plusMonths(2);
    private static final LocalDate NOT_PREGNANT = null;

    @Mock
    private EligibilityAndEntitlementService eligibilityAndEntitlementService;
    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private PaymentCycleService paymentCycleService;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private DetermineEntitlementNotificationHandler determineEntitlementNotificationHandler;
    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    @InjectMocks
    private DetermineEntitlementMessageProcessor processor;

    @Test
    void shouldSuccessfullyProcessMessageAndTriggerPaymentWhenClaimantIsEligible() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext(EXPECTED_DELIVERY_DATE);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateExistingClaimant(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verify(paymentCycleService).updatePaymentCycle(context.getCurrentPaymentCycle(), decision);
        MessagePayload expectedPayload = MessagePayloadFactory.buildMakePaymentMessagePayload(context.getCurrentPaymentCycle());
        verify(messageQueueClient).sendMessage(expectedPayload, MessageType.MAKE_PAYMENT);
        verifyZeroInteractions(claimRepository, pregnancyEntitlementCalculator);
    }

    @Test
    void shouldExpireClaimAndSendNoLongerOnSchemeEmailWhenEligibleWithNoChildrenAndNotPregnant() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext(NOT_PREGNANT);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityAndEntitlementDecision decision = anIneligibleDecisionWithNoChildren();
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateExistingClaimant(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verify(paymentCycleService).updatePaymentCycle(context.getCurrentPaymentCycle(), decision);
        verifyClaimSavedAtStatus(ClaimStatus.EXPIRED);
        verify(determineEntitlementNotificationHandler).sendNoChildrenOnFeedClaimNoLongerEligibleEmail(context.getClaim());
        verifyZeroInteractions(messageQueueClient);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(NOT_PREGNANT, context.getCurrentPaymentCycle().getCycleStartDate());
    }

    @ParameterizedTest
    @CsvSource({"INELIGIBLE", "PENDING", "NO_MATCH"})
    void shouldUpdateClaimWhenClaimantIsNotEligible(EligibilityStatus eligibilityStatus) {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext(NOT_PREGNANT);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(eligibilityStatus);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateExistingClaimant(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verify(paymentCycleService).updatePaymentCycle(context.getCurrentPaymentCycle(), decision);
        verifyClaimSavedAtStatus(ClaimStatus.PENDING_EXPIRY);
        verify(determineEntitlementNotificationHandler).sendClaimNoLongerEligibleEmail(context.getClaim());
        verifyZeroInteractions(pregnancyEntitlementCalculator);
    }

    private void verifyClaimSavedAtStatus(ClaimStatus claimStatus) {
        ArgumentCaptor<Claim> argumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(argumentCaptor.capture());
        Claim claim = argumentCaptor.getValue();
        assertThat(claim.getClaimStatus()).isEqualTo(claimStatus);
    }

    private DetermineEntitlementMessageContext buildMessageContext(LocalDate expectedDeliveryDate) {
        //Claim
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);

        //Current payment cycle
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claim);

        //Previous payment cycle
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claim);

        return aDetermineEntitlementMessageContext(
                currentPaymentCycle,
                previousPaymentCycle,
                claim);
    }

}
