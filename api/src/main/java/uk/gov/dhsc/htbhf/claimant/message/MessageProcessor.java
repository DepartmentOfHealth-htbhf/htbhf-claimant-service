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
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

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

            processMessages(messageTypeProcessor, allMessagesOfType);
        }
    }

    private void processMessages(MessageTypeProcessor messageTypeProcessor, List<Message> allMessagesOfType) {
        Map<MessageStatus, Long> statuses = allMessagesOfType.stream()
                .map(messageTypeProcessor::processMessage)
                .peek(messageStatus -> checkForNullMessageStatus(messageTypeProcessor, messageStatus))
                .filter(Objects::nonNull)
                .collect(groupingBy(identity(), counting()));

        statuses.forEach((messageStatus, count) -> log.info("Processed {} {} with status {}", count, count == 1 ? "message" : "messages", messageStatus.name()));
    }

    private void checkForNullMessageStatus(MessageTypeProcessor messageTypeProcessor, MessageStatus messageStatus) {
        if (messageStatus == null) {
            log.error("Received null message status from message processor supporting type: {}", messageTypeProcessor.supportsMessageType());
        }
    }

}
