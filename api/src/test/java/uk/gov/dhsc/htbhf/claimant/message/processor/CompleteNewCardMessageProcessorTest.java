package uk.gov.dhsc.htbhf.claimant.message.processor;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessingException;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.context.CompleteNewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_NEW_TO_ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidCompleteNewCardMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.MESSAGE_PAYLOAD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithPayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
@Transactional
class CompleteNewCardMessageProcessorTest {

    @MockBean
    private MessageContextLoader messageContextLoader;
    @MockBean
    private PaymentCycleService paymentCycleService;
    @MockBean
    private MessageQueueDAO messageQueueDAO;
    @MockBean
    private ClaimRepository claimRepository;
    @MockBean
    private EventAuditor eventAuditor;
    @MockBean
    private ClaimMessageSender claimMessageSender;

    @Autowired
    private CompleteNewCardMessageProcessor completeNewCardMessageProcessor;

    @Test
    void shouldRollBackTransactionAndReturnErrorWhenExceptionIsThrown() {
        //Given
        MessageProcessingException testException = new MessageProcessingException("Error reading value");
        given(messageContextLoader.loadCompleteNewCardContext(any())).willThrow(testException);
        Message message = aValidMessageWithPayload(MESSAGE_PAYLOAD);

        //When
        MessageProcessingException thrown = catchThrowableOfType(
                () -> completeNewCardMessageProcessor.processMessage(message),
                MessageProcessingException.class);

        //Then
        assertThat(thrown).isEqualTo(testException);
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
        verify(messageContextLoader).loadCompleteNewCardContext(message);
        verifyNoMoreInteractions(claimRepository);
    }

    @Test
    void shouldProcessCompleteNewCardMessage() {
        //Given
        CompleteNewCardMessageContext context = aValidCompleteNewCardMessageContext();
        Claim claim = context.getClaim();
        LocalDate cycleStartDate = claim.getClaimStatusTimestamp().toLocalDate();
        given(messageContextLoader.loadCompleteNewCardContext(any())).willReturn(context);
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);
        given(paymentCycleService.createAndSavePaymentCycleForEligibleClaim(any(), any(), any())).willReturn(paymentCycle);
        Message message = aValidMessage();

        //When
        MessageStatus status = completeNewCardMessageProcessor.processMessage(message);

        //Then
        assertThat(status).isEqualTo(COMPLETED);
        assertThat(claim.getCardAccountId()).isEqualTo(context.getCardAccountId());
        assertThat(claim.getClaimStatus()).isEqualTo(ACTIVE);
        assertThat(TestTransaction.isActive()).isTrue();

        verify(messageContextLoader).loadCompleteNewCardContext(message);
        verify(claimRepository).save(claim);
        verify(eventAuditor).auditNewCard(claim.getId(), context.getCardAccountId());
        verify(claimMessageSender)
                .sendReportClaimMessage(claim, context.getEligibilityAndEntitlementDecision().getDateOfBirthOfChildren(), UPDATED_FROM_NEW_TO_ACTIVE);
        verify(paymentCycleService).createAndSavePaymentCycleForEligibleClaim(claim, cycleStartDate, context.getEligibilityAndEntitlementDecision());
        verifyMakeFirstPaymentMessageSent(claim, paymentCycle);
    }

    private void verifyMakeFirstPaymentMessageSent(Claim claim, PaymentCycle paymentCycle) {
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueDAO).sendMessage(payloadCaptor.capture(), eq(MessageType.MAKE_FIRST_PAYMENT));
        assertThat(payloadCaptor.getValue()).isInstanceOf(MakePaymentMessagePayload.class);
        MakePaymentMessagePayload payload = (MakePaymentMessagePayload) payloadCaptor.getValue();
        assertThat(payload.getCardAccountId()).isEqualTo(claim.getCardAccountId());
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
    }

}
