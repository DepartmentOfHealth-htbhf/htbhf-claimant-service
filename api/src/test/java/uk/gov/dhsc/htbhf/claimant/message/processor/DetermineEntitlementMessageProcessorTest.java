package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.communications.ClaimEmailHandler;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
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
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithNoChildrenAndNotPregnant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aDetermineEntitlementMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartDateAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildren;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@ExtendWith(MockitoExtension.class)
class DetermineEntitlementMessageProcessorTest {

    private static final LocalDate EXPECTED_DELIVERY_DATE = LocalDate.now().plusMonths(2);

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
    private ClaimEmailHandler claimEmailHandler;
    @Mock
    private EventAuditor eventAuditor;

    @InjectMocks
    private DetermineEntitlementMessageProcessor processor;

    @Test
    void shouldSuccessfullyProcessMessageAndTriggerPaymentWhenClaimantIsEligible() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext();
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
        verifyZeroInteractions(claimRepository);
    }

    @ParameterizedTest
    @CsvSource({"INELIGIBLE", "PENDING", "NO_MATCH"})
    void shouldUpdateClaimWhenClaimantIsNotEligible(EligibilityStatus eligibilityStatus) {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext();
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
        verifyClaimSavedWithStatus(PENDING_EXPIRY);
        verify(claimEmailHandler).sendClaimNoLongerEligibleEmail(context.getClaim());
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE", "PENDING_EXPIRY"})
    void shouldSetClaimToExpiredWhenClaimantIsNotPregnantAndHasNoChildrenUnderFour(ClaimStatus claimStatus) {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContextWithNoChildrenAndNotPregnant(claimStatus);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        PaymentCycleVoucherEntitlement voucherEntitlement
                = aPaymentCycleVoucherEntitlementMatchingChildren(context.getCurrentPaymentCycle().getCycleStartDate(), emptyList());
        EligibilityAndEntitlementDecision decision = aDecisionWithEntitlement(voucherEntitlement);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

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
        verifyClaimSavedWithStatus(EXPIRED);
        verify(eventAuditor).auditClaimExpired(context.getClaim());
    }

    private void verifyClaimSavedWithStatus(ClaimStatus claimStatus) {
        ArgumentCaptor<Claim> argumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(argumentCaptor.capture());
        Claim claim = argumentCaptor.getValue();
        assertThat(claim.getClaimStatus()).isEqualTo(claimStatus);
    }

    private DetermineEntitlementMessageContext buildMessageContextWithNoChildrenAndNotPregnant(ClaimStatus claimStatus) {
        Claimant claimant = aClaimantWithNoChildrenAndNotPregnant();
        Claim claim = aValidClaimBuilder()
                .claimant(claimant)
                .claimStatus(claimStatus)
                .build();

        return buildMessageContext(claim);
    }

    private DetermineEntitlementMessageContext buildMessageContext() {
        Claim claim = aClaimWithExpectedDeliveryDate(EXPECTED_DELIVERY_DATE);
        return buildMessageContext(claim);
    }

    private DetermineEntitlementMessageContext buildMessageContext(Claim claim) {
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claim);

        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claim);

        return aDetermineEntitlementMessageContext(
                currentPaymentCycle,
                previousPaymentCycle,
                claim);
    }

}
