package uk.gov.dhsc.htbhf.claimant.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;

import java.io.IOException;

/**
 * Utility class that encapsulated transforming a message payload into the required payload class.
 */
@Component
@AllArgsConstructor
public class PayloadMapper {

    private ObjectMapper objectMapper;

    public <T> T getPayload(Message message, Class<T> payloadClass) {
        try {
            return objectMapper.readValue(message.getMessagePayload(), payloadClass);
        } catch (IOException e) {
            throw new MessageProcessingException(
                    String.format("Unable to create message payload for message with id: %s, payload is: %s", message.getId(), message.getMessagePayload()),
                    e);
        }
    }
}
