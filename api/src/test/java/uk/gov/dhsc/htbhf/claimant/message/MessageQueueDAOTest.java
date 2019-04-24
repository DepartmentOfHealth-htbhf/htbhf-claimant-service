package uk.gov.dhsc.htbhf.claimant.message;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.PAYLOAD_JSON;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aValidNewCardRequestMessagePayload;

@ExtendWith(MockitoExtension.class)
class MessageQueueDAOTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageQueueDAO messageQueueDAO;

    @Test
    void sendNewCardRequestMessage() throws JsonProcessingException {
        //Given
        NewCardRequestMessagePayload payload = aValidNewCardRequestMessagePayload();
        given(objectMapper.writeValueAsString(any(NewCardRequestMessagePayload.class))).willReturn(PAYLOAD_JSON);
        //When
        messageQueueDAO.sendMessage(payload, CREATE_NEW_CARD);

        //Then
        verify(objectMapper).writeValueAsString(payload);
        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getAllValues()).hasSize(1);
        Message actualMessage = messageArgumentCaptor.getValue();
        assertThat(actualMessage.getMessagePayload()).isEqualTo(PAYLOAD_JSON);
        assertThat(actualMessage.getMessageType()).isEqualTo(CREATE_NEW_CARD);
        assertThat(actualMessage.getMessageTimestamp()).isNotNull();
    }

    @Test
    void sendNewCardRequestMessageFailsJsonParsing() throws JsonProcessingException {
        //Given
        NewCardRequestMessagePayload payload = aValidNewCardRequestMessagePayload();
        JsonParseException testException = new JsonParseException(null, "Test exception");
        given(objectMapper.writeValueAsString(any(NewCardRequestMessagePayload.class))).willThrow(testException);
        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> messageQueueDAO.sendMessage(payload, CREATE_NEW_CARD), MessageProcessingException.class);

        //Then
        assertThat(thrown)
                .hasMessage("Unable to create JSON payload from object")
                .hasCause(testException);
        verifyZeroInteractions(messageRepository);
        verify(objectMapper).writeValueAsString(payload);
    }
}
