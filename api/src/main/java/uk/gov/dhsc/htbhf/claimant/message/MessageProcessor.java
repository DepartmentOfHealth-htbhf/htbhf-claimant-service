package uk.gov.dhsc.htbhf.claimant.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;

/**
 * Component that is triggered on a schedule and is responsible for finding all the messages that need to be
 * processed and passing them off to the {@link MessageTypeProcessor} matching the {@link MessageType} of
 * the message stored in the database.
 */
//TODO MRS 2019-04-23: Make this be constructed in a Configuration class, so construction of Map is in there, not here.
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageProcessor {

    private final List<MessageTypeProcessor> allMessageProcessors;
    private final MessageRepository messageRepository;

    private Map<MessageType, MessageTypeProcessor> allMessageProcessorsByType;

    @PostConstruct
    public void buildMessageTypeProcessorMap() {
        if (CollectionUtils.isEmpty(allMessageProcessors)) {
            //TODO MRS 2019-04-23: When we have the first MessageTypeProcessor, change this check to throw an Exception, application should fail to startup
            log.warn("No MessageTypeProcessors found in application context, we currently have no support for any types of messages");
            return;
        }
        allMessageProcessorsByType = allMessageProcessors
                .stream()
                .collect(Collectors.toMap(MessageTypeProcessor::supportsMessageType, messageTypeProcessor -> messageTypeProcessor));
        warnForMissingProcessorType();
    }


    //Add a useful message to the logs detailing which messages currently do not have a processor configured in the application context.
    //Have added the PMD suppression because it can't see the guard outside of the Stream.
    @SuppressWarnings("PMD.GuardLogStatement")
    private void warnForMissingProcessorType() {
        if (log.isWarnEnabled()) {
            Stream.of(MessageType.values()).forEach(messageType -> {
                if (!allMessageProcessorsByType.containsKey(messageType)) {
                    log.warn("We currently have no support for messages of type [{}], "
                            + "no MessageTypeProcessor implementation found in application context on startup", messageType);
                }
            });
        }
    }

    //TODO MRS 2019-04-18: This will be triggered on a schedule, we might want to break this down so we can define a schedule per message type
    public void processAllMessages() {
        Stream.of(MessageType.values()).forEach(this::processMessagesOfType);
    }

    private void processMessagesOfType(MessageType messageType) {
        List<Message> allMessagesOfType = messageRepository.findAllMessagesByTypeOrderedByDate(messageType);
        if (!CollectionUtils.isEmpty(allMessagesOfType)) {
            MessageTypeProcessor messageTypeProcessor = allMessageProcessorsByType.get(messageType);
            if (messageTypeProcessor == null) {
                throw new IllegalArgumentException("No message type processor found in application context for message type: "
                        + messageType + ", there are " + allMessagesOfType.size() + " message(s) in the queue");
            }
            allMessagesOfType.forEach(messageTypeProcessor::processMessage);
        }
        //TODO MRS 2019-04-18: Do a simple logging of total number of messages run and their status.
    }

}
