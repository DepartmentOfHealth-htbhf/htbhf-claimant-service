package uk.gov.dhsc.htbhf.claimant.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.time.LocalDateTime;

/**
 * Responsible for building and storing the complete {@link Message} entities, which involves building the JSON String
 * from the provided object. The objects representing the payload all implement the
 * {@link uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload} marker interface.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageQueueDAO implements MessageQueueClient {

    private final MessageRepository messageRepository;

    private final ObjectMapper objectMapper;

    @Override
    public void sendMessage(MessagePayload messagePayload, MessageType messageType) {
        try {
            messageRepository.save(buildMessage(messagePayload, messageType));
        } catch (JsonProcessingException e) {
            throw new MessageProcessingException("Unable to create JSON payload from object", e);
        }
    }

    private Message buildMessage(MessagePayload messagePayload, MessageType messageType) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(messagePayload);
        return Message.builder()
                .messagePayload(json)
                .messageType(messageType)
                .messageTimestamp(LocalDateTime.now())
                .build();
    }

}
