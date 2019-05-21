package uk.gov.dhsc.htbhf.claimant.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@Slf4j
public class MessageProcessorConfiguration {

    @Bean
    public MessageProcessor messageProcessor(List<MessageTypeProcessor> messageProcessors,
                                             MessageRepository messageRepository,
                                             MessageStatusProcessor messageStatusProcessor,
                                             @Value("${message-processor.message-limit}") int messageProcessingLimit) {
        return new MessageProcessor(messageStatusProcessor, messageRepository, buildMessageTypeProcessorMap(messageProcessors), messageProcessingLimit);
    }

    private Map<MessageType, MessageTypeProcessor> buildMessageTypeProcessorMap(List<MessageTypeProcessor> messageProcessors) {
        if (CollectionUtils.isEmpty(messageProcessors)) {
            log.error("No MessageTypeProcessors found in application context, we currently have no support for any types of messages");
            throw new BeanCreationException("Unable to create MessageProcessor, no MessageTypeProcessor instances found");
        }
        Map<MessageType, MessageTypeProcessor> messageProcessorsByType = messageProcessors
                .stream()
                .collect(Collectors.toMap(MessageTypeProcessor::supportsMessageType, messageTypeProcessor -> messageTypeProcessor));
        warnForMissingProcessorType(messageProcessorsByType);
        return messageProcessorsByType;
    }

    //Add a useful message to the logs detailing which messages currently do not have a processor configured in the application context.
    private void warnForMissingProcessorType(Map<MessageType, MessageTypeProcessor> messageProcessorsByType) {
        Stream.of(MessageType.values()).forEach(messageType -> {
            if (!messageProcessorsByType.containsKey(messageType)) {
                log.warn("We currently have no support for messages of type [{}], no MessageTypeProcessor implementation found in context on startup",
                        messageType);
            }
        });
    }

}
