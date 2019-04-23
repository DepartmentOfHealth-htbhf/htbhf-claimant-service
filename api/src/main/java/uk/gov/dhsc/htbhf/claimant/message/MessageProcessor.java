package uk.gov.dhsc.htbhf.claimant.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Component that is triggered on a schedule and is responsible for finding all the messages that need to be
 * processed and passing them off to the {@link MessageTypeProcessor} matching the {@link MessageType} of
 * the message stored in the database.
 */
@RequiredArgsConstructor
@Slf4j
public class MessageProcessor {

    private final MessageRepository messageRepository;
    private final Map<MessageType, MessageTypeProcessor> messageProcessorsByType;

    @Scheduled(cron = "${message-processor.cron-schedule}")
    @SchedulerLock(
            name = "Process all messages",
            lockAtLeastForString = "${message-processor.minimum-lock-time}",
            lockAtMostForString = "${message-processor.maximum-lock-time}")
    public void processAllMessages() {
        Stream.of(MessageType.values()).forEach(this::processMessagesOfType);
    }

    private void processMessagesOfType(MessageType messageType) {
        List<Message> allMessagesOfType = messageRepository.findAllMessagesByTypeOrderedByDate(messageType);
        if (!CollectionUtils.isEmpty(allMessagesOfType)) {
            MessageTypeProcessor messageTypeProcessor = messageProcessorsByType.get(messageType);
            if (messageTypeProcessor == null) {
                throw new IllegalArgumentException("No message type processor found in application context for message type: "
                        + messageType + ", there are " + allMessagesOfType.size() + " message(s) in the queue");
            }
            allMessagesOfType.forEach(messageTypeProcessor::processMessage);
        }
        //TODO MRS 2019-04-18: Do a simple logging of total number of messages run and their status.
    }

}
