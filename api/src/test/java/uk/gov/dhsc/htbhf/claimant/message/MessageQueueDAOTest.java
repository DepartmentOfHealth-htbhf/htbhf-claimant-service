package uk.gov.dhsc.htbhf.claimant.message;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.payload.RequestNewCardMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.NEW_CARD_PAYLOAD_JSON;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aValidNewCardRequestMessagePayload;

@SpringBootTest
@Transactional
@AutoConfigureEmbeddedDatabase
class MessageQueueDAOTest {

    @MockBean
    private MessageRepository messageRepository;

    @MockBean
    private ObjectMapper objectMapper;

    @Autowired
    private MessageQueueDAO messageQueueDAO;

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void sendNewCardRequestMessage() throws JsonProcessingException {
        //Given
        RequestNewCardMessagePayload payload = aValidNewCardRequestMessagePayload();
        given(objectMapper.writeValueAsString(any(RequestNewCardMessagePayload.class))).willReturn(NEW_CARD_PAYLOAD_JSON);
        LocalDateTime testStart = LocalDateTime.now();

        //When
        messageQueueDAO.sendMessage(payload, REQUEST_NEW_CARD);

        //Then
        verify(objectMapper).writeValueAsString(payload);
        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getAllValues()).hasSize(1);
        Message actualMessage = messageArgumentCaptor.getValue();
        assertThat(actualMessage.getMessagePayload()).isEqualTo(NEW_CARD_PAYLOAD_JSON);
        assertThat(actualMessage.getMessageType()).isEqualTo(REQUEST_NEW_CARD);
        assertThat(actualMessage.getProcessAfter()).isAfterOrEqualTo(testStart);
        assertThat(actualMessage.getStatus()).isEqualTo(MessageStatus.NEW);
        assertThat(TestTransaction.isActive()).isTrue();
    }

    @Test
    void sendNewCardRequestMessageFailsJsonParsing() throws JsonProcessingException {
        //Given
        RequestNewCardMessagePayload payload = aValidNewCardRequestMessagePayload();
        JsonParseException testException = new JsonParseException(null, "Test exception");
        given(objectMapper.writeValueAsString(any(RequestNewCardMessagePayload.class))).willThrow(testException);
        //When
        MessageProcessingException thrown = catchThrowableOfType(
                () -> messageQueueDAO.sendMessage(payload, REQUEST_NEW_CARD),
                MessageProcessingException.class);

        //Then
        assertThat(thrown)
                .hasMessage("Unable to create JSON payload from object")
                .hasCause(testException);
        verifyNoMoreInteractions(messageRepository);
        verify(objectMapper).writeValueAsString(payload);
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void shouldSendMessageWithGivenDelay() throws JsonProcessingException {
        //Given
        RequestNewCardMessagePayload payload = aValidNewCardRequestMessagePayload();
        given(objectMapper.writeValueAsString(any(RequestNewCardMessagePayload.class))).willReturn(NEW_CARD_PAYLOAD_JSON);
        Duration messageDelay = Duration.ofDays(3);
        LocalDateTime testStart = LocalDateTime.now();

        //When
        messageQueueDAO.sendMessageWithDelay(payload, REQUEST_NEW_CARD, messageDelay);

        //Then
        verify(objectMapper).writeValueAsString(payload);
        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getAllValues()).hasSize(1);
        Message actualMessage = messageArgumentCaptor.getValue();
        assertThat(actualMessage.getMessagePayload()).isEqualTo(NEW_CARD_PAYLOAD_JSON);
        assertThat(actualMessage.getMessageType()).isEqualTo(REQUEST_NEW_CARD);
        assertThat(actualMessage.getProcessAfter()).isAfterOrEqualTo(testStart.plus(messageDelay));
        assertThat(actualMessage.getStatus()).isEqualTo(MessageStatus.NEW);
        assertThat(TestTransaction.isActive()).isTrue();
    }
}
