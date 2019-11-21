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
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.RequestNewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.service.RequestNewCardService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidNewCardMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.MESSAGE_PAYLOAD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithPayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
@Transactional
class RequestNewCardMessageProcessorTest {

    @MockBean
    private RequestNewCardService requestNewCardService;
    @MockBean
    private MessageContextLoader messageContextLoader;
    @MockBean
    private PaymentCycleService paymentCycleService;
    @MockBean
    private MessageQueueDAO messageQueueDAO;

    @Autowired
    private RequestNewCardMessageProcessor requestNewCardMessageProcessor;

    @Test
    void shouldRollBackTransactionAndReturnErrorWhenExceptionIsThrown() {
        //Given
        MessageProcessingException testException = new MessageProcessingException("Error reading value");
        given(messageContextLoader.loadRequestNewCardContext(any())).willThrow(testException);
        Message message = aValidMessageWithPayload(MESSAGE_PAYLOAD);

        //When
        MessageProcessingException thrown = catchThrowableOfType(
                () -> requestNewCardMessageProcessor.processMessage(message),
                MessageProcessingException.class);

        //Then
        assertThat(thrown).isEqualTo(testException);
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
        verify(messageContextLoader).loadRequestNewCardContext(message);
        verifyNoInteractions(requestNewCardService);
    }

    @Test
    void shouldProcessNewCardMessageAndDeleteMessage() {
        //Given
        RequestNewCardMessageContext context = aValidNewCardMessageContext();
        LocalDate cycleStartDate = context.getClaim().getClaimStatusTimestamp().toLocalDate();
        given(messageContextLoader.loadRequestNewCardContext(any())).willReturn(context);
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(context.getClaim());
        given(paymentCycleService.createAndSavePaymentCycleForEligibleClaim(any(), any(), any())).willReturn(paymentCycle);
        Message message = aValidMessage();

        //When
        MessageStatus status = requestNewCardMessageProcessor.processMessage(message);

        //Then
        assertThat(status).isEqualTo(COMPLETED);
        assertThat(TestTransaction.isActive()).isTrue();
        verify(messageContextLoader).loadRequestNewCardContext(message);
        verify(requestNewCardService).createNewCard(context.getClaim(), context.getEligibilityAndEntitlementDecision().getDateOfBirthOfChildren());
        verify(paymentCycleService).createAndSavePaymentCycleForEligibleClaim(
                context.getClaim(),
                cycleStartDate,
                context.getEligibilityAndEntitlementDecision());
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verifyMakeFirstPaymentMessageSent(payloadCaptor, context.getClaim(), paymentCycle);
    }

    private void verifyMakeFirstPaymentMessageSent(ArgumentCaptor<MessagePayload> payloadCaptor, Claim claim, PaymentCycle paymentCycle) {
        verify(messageQueueDAO).sendMessage(payloadCaptor.capture(), eq(MessageType.MAKE_FIRST_PAYMENT));
        assertThat(payloadCaptor.getValue()).isInstanceOf(MakePaymentMessagePayload.class);
        MakePaymentMessagePayload payload = (MakePaymentMessagePayload) payloadCaptor.getValue();
        assertThat(payload.getCardAccountId()).isEqualTo(claim.getCardAccountId());
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
    }

}
