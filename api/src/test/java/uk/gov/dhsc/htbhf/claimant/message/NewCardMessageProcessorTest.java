package uk.gov.dhsc.htbhf.claimant.message;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aValidNewCardRequestMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.MESSAGE_PAYLOAD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithPayload;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
@Transactional
class NewCardMessageProcessorTest {

    @MockBean
    private NewCardService newCardService;

    @MockBean
    private PayloadMapper payloadMapper;

    @MockBean
    private MessageRepository messageRepository;

    @Autowired
    private NewCardMessageProcessor newCardMessageProcessor;

    @Test
    void shouldRollBackTransactionAndReturnErrorWhenExceptionIsThrown() {
        //Given
        MessageProcessingException testException = new MessageProcessingException("Error reading value");
        given(payloadMapper.getPayload(any(), eq(NewCardRequestMessagePayload.class))).willThrow(testException);
        Message message = aValidMessageWithPayload(MESSAGE_PAYLOAD);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> newCardMessageProcessor.processMessage(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).isEqualTo(testException);
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
        verify(payloadMapper).getPayload(message, NewCardRequestMessagePayload.class);
        verifyZeroInteractions(newCardService, messageRepository);
    }

    @Test
    void shouldCreateNewCardAndDeleteMessage() {
        //Given
        NewCardRequestMessagePayload payload = aValidNewCardRequestMessagePayload();
        given(payloadMapper.getPayload(any(), eq(NewCardRequestMessagePayload.class))).willReturn(payload);
        Message message = aValidMessage();

        //When
        MessageStatus status = newCardMessageProcessor.processMessage(message);

        //Then
        assertThat(status).isEqualTo(COMPLETED);
        assertThat(TestTransaction.isActive()).isTrue();
        verify(payloadMapper).getPayload(message, NewCardRequestMessagePayload.class);
        verify(newCardService).createNewCard(payload.getClaimId());
        verify(messageRepository).delete(message);
    }
}
