package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityService;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibilityAndEntitlementDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aDetermineEntitlementMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aMakePaymentPayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleStartDateAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleStartDateEntitlementAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class DetermineEntitlementMessageProcessorTest {

    @Mock
    private EligibilityService eligibilityService;
    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private PaymentCycleRepository paymentCycleRepository;
    @Mock
    private MessageQueueDAO messageQueueDAO;

    @InjectMocks
    private DetermineEntitlementMessageProcessor processor;

    @Test
    void shouldSuccessfullyProcessMessageAndTriggerPaymentForEligibleClaimant() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext();
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityAndEntitlementDecision eligibility = anEligibilityAndEntitlementDecisionWithStatus(ELIGIBLE);
        given(eligibilityService.determineEligibilityAndEntitlementForExistingClaimant(any(), any(), any())).willReturn(eligibility);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityService).determineEligibilityAndEntitlementForExistingClaimant(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verifyPaymentCycleUpdatedSuccessfully(context.getCurrentPaymentCycle().getId(), eligibility.getVoucherEntitlement(), ELIGIBLE);
        MakePaymentMessagePayload expectedPaymentMessagePayload = aMakePaymentPayload(context.getClaim().getId(), context.getCurrentPaymentCycle().getId());
        verify(messageQueueDAO).sendMessage(expectedPaymentMessagePayload, MAKE_PAYMENT);
    }

    @Test
    void shouldProcessMessageButNotTriggerPaymentForClaimantNoLongerEligible() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext();
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityAndEntitlementDecision eligibility = anEligibilityAndEntitlementDecisionWithStatus(INELIGIBLE);
        given(eligibilityService.determineEligibilityAndEntitlementForExistingClaimant(any(), any(), any())).willReturn(eligibility);

        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityService).determineEligibilityAndEntitlementForExistingClaimant(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verifyPaymentCycleUpdatedSuccessfully(context.getCurrentPaymentCycle().getId(), null, INELIGIBLE);
        verifyZeroInteractions(messageQueueDAO);
    }

    private DetermineEntitlementMessageContext buildMessageContext() {
        //Claim
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(1);
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);

        //Current payment cycle
        LocalDate cycleStartDate = LocalDate.now();
        PaymentCycle currentPaymentCycle = aPaymentCycleWithCycleStartDateAndClaim(cycleStartDate, claim);

        //Previous payment cycle
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        VoucherEntitlement previousVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(previousCycleStartDate);
        PaymentCycleVoucherEntitlement previousPaymentCycleVoucherEntitlement = aPaymentCycleVoucherEntitlementWithEntitlement(previousVoucherEntitlement);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithCycleStartDateEntitlementAndClaim(previousCycleStartDate,
                previousPaymentCycleVoucherEntitlement,
                claim);

        return aDetermineEntitlementMessageContext(
                currentPaymentCycle,
                previousPaymentCycle,
                claim);
    }

    private void verifyPaymentCycleUpdatedSuccessfully(UUID currentPaymentCycleId,
                                                       PaymentCycleVoucherEntitlement voucherEntitlement,
                                                       EligibilityStatus eligibilityStatus) {
        ArgumentCaptor<PaymentCycle> paymentCycleArgumentCaptor = ArgumentCaptor.forClass(PaymentCycle.class);
        verify(paymentCycleRepository).save(paymentCycleArgumentCaptor.capture());
        PaymentCycle updatedPaymentCycle = paymentCycleArgumentCaptor.getValue();
        assertThat(updatedPaymentCycle).isNotNull();
        assertThat(updatedPaymentCycle.getId()).isEqualTo(currentPaymentCycleId);
        assertThat(updatedPaymentCycle.getVoucherEntitlement()).isEqualTo(voucherEntitlement);
        assertThat(updatedPaymentCycle.getEligibilityStatus()).isEqualTo(eligibilityStatus);
    }
}
