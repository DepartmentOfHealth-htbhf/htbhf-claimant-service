package uk.gov.dhsc.htbhf.claimant.message.processor;

import com.google.common.collect.ImmutableList;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.dhsc.htbhf.claimant.entitlement.CycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessingException;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityService;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aDetermineEntitlementMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aMakePaymentPayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleStartDateAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleStartDateEntitlementAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aValidPaymentCycleVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.LISA_DOB;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.MAGGIE_DOB;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
@Transactional
class DetermineEntitlementMessageProcessorTest {

    @MockBean
    private MessageRepository messageRepository;
    @MockBean
    private EligibilityService eligibilityService;
    @MockBean
    private CycleEntitlementCalculator cycleEntitlementCalculator;
    @MockBean
    private MessageContextLoader messageContextLoader;
    @MockBean
    private PaymentCycleRepository paymentCycleRepository;
    @MockBean
    private MessageQueueDAO messageQueueDAO;

    @Autowired
    private DetermineEntitlementMessageProcessor processor;

    @Test
    void shouldRollBackTransactionAndReturnErrorWhenExceptionIsThrown() {
        //Given
        MessageProcessingException testException = new MessageProcessingException("Error reading value");
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willThrow(testException);
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> processor.processMessage(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).isEqualTo(testException);
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verifyZeroInteractions(eligibilityService, messageRepository, cycleEntitlementCalculator, messageContextLoader, paymentCycleRepository);
    }

    @Test
    void shouldSuccessfullyProcessMessageAndTriggerPaymentForEligibleClaimant() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext();
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        given(eligibilityService.determineEligibility(any())).willReturn(eligibilityResponse);

        //Current payment cycle voucher entitlement mocking
        PaymentCycleVoucherEntitlement currentPaymentCycleVoucherEntitlement = aValidPaymentCycleVoucherEntitlement();
        given(cycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(currentPaymentCycleVoucherEntitlement);
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        assertThat(TestTransaction.isActive()).isTrue();
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityService).determineEligibility(context.getClaim().getClaimant());

        List<LocalDate> dateOfBirthOfChildren = ImmutableList.of(MAGGIE_DOB, LISA_DOB);
        verify(cycleEntitlementCalculator).calculateEntitlement(
                Optional.of(context.getClaim().getClaimant().getExpectedDeliveryDate()),
                dateOfBirthOfChildren,
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle().getVoucherEntitlement());

        verifyPaymentCycleUpdatedSuccessfully(context.getCurrentPaymentCycle().getId(), currentPaymentCycleVoucherEntitlement, ELIGIBLE);
        MakePaymentMessagePayload expectedPaymentMessagePayload = aMakePaymentPayload(context.getClaim().getId(), context.getCurrentPaymentCycle().getId());
        verify(messageQueueDAO).sendMessage(expectedPaymentMessagePayload, MAKE_PAYMENT);
        verify(messageRepository).delete(message);
    }

    @Test
    void shouldProcessMessageButNotTriggerPaymentForClaimantNoLongerEligible() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext();
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(INELIGIBLE);
        given(eligibilityService.determineEligibility(any())).willReturn(eligibilityResponse);

        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        assertThat(TestTransaction.isActive()).isTrue();
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityService).determineEligibility(context.getClaim().getClaimant());

        verifyPaymentCycleUpdatedSuccessfully(context.getCurrentPaymentCycle().getId(), null, INELIGIBLE);
        verify(messageRepository).delete(message);
        verifyZeroInteractions(cycleEntitlementCalculator, messageQueueDAO);
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
