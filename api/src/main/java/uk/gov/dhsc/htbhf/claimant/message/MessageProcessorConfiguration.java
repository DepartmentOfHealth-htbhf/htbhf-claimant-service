package uk.gov.dhsc.htbhf.claimant.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

@Configuration
@Slf4j
public class MessageProcessorConfiguration {

    @Bean
    public MessageProcessor messageProcessor(List<MessageTypeProcessor> messageProcessors, MessageRepository messageRepository) {
        return new MessageProcessor(messageRepository, buildMessageTypeProcessorMap(messageProcessors));
    }

    private Map<MessageType, MessageTypeProcessor> buildMessageTypeProcessorMap(List<MessageTypeProcessor> messageProcessors) {
        if (CollectionUtils.isEmpty(messageProcessors)) {
            //TODO MRS 2019-04-23: When we have the first MessageTypeProcessor, change this check to throw an Exception, application should fail to startup
            log.warn("No MessageTypeProcessors found in application context, we currently have no support for any types of messages");
            return emptyMap();
        }
        Map<MessageType, MessageTypeProcessor> messageProcessorsByType = messageProcessors
                .stream()
                .collect(Collectors.toMap(MessageTypeProcessor::supportsMessageType, messageTypeProcessor -> messageTypeProcessor));
        warnForMissingProcessorType(messageProcessorsByType);
        return messageProcessorsByType;
    }

    //Add a useful message to the logs detailing which messages currently do not have a processor configured in the application context.
    //Have added the PMD suppression because it can't see the guard outside of the Stream.
    @SuppressWarnings("PMD.GuardLogStatement")
    private void warnForMissingProcessorType(Map<MessageType, MessageTypeProcessor> messageProcessorsByType) {
        if (log.isWarnEnabled()) {
            Stream.of(MessageType.values()).forEach(messageType -> {
                if (!messageProcessorsByType.containsKey(messageType)) {
                    log.warn("We currently have no support for messages of type [{}], "
                            + "no MessageTypeProcessor implementation found in application context on startup", messageType);
                }
            });
        }
    }

}
