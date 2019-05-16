package uk.gov.dhsc.htbhf.claimant.message.processor;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessingException;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.NewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidNewCardMessageContext;
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
    private MessageContextLoader messageContextLoader;

    @MockBean
    private MessageRepository messageRepository;

    @Autowired
    private NewCardMessageProcessor newCardMessageProcessor;

    @Test
    void shouldRollBackTransactionAndReturnErrorWhenExceptionIsThrown() {
        //Given
        MessageProcessingException testException = new MessageProcessingException("Error reading value");
        given(messageContextLoader.loadNewCardContext(any())).willThrow(testException);
        Message message = aValidMessageWithPayload(MESSAGE_PAYLOAD);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> newCardMessageProcessor.processMessage(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).isEqualTo(testException);
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
        verify(messageContextLoader).loadNewCardContext(message);
        verifyZeroInteractions(newCardService, messageRepository);
    }

    @Test
    void shouldCreateNewCardAndDeleteMessage() {
        //Given
        NewCardMessageContext context = aValidNewCardMessageContext();
        given(messageContextLoader.loadNewCardContext(any())).willReturn(context);
        Message message = aValidMessage();

        //When
        MessageStatus status = newCardMessageProcessor.processMessage(message);

        //Then
        assertThat(status).isEqualTo(COMPLETED);
        assertThat(TestTransaction.isActive()).isTrue();
        verify(messageContextLoader).loadNewCardContext(message);
        verify(newCardService).createNewCard(context.getClaim());
        verify(messageRepository).delete(message);
    }

}
