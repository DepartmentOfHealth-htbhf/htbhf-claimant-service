package uk.gov.dhsc.htbhf.claimant.message;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.ERROR;

@Component
public class MessageStatusProcessor {

    private static final long THIRTY_SECONDS = 30;
    private static final int MAX_EXPONENT_SIZE = 50;

    private final MessageRepository messageRepository;
    private final long maximumRetryDelaySeconds;

    public MessageStatusProcessor(
            MessageRepository messageRepository,
            @Value("${message-processor.maximum-retry-delay-seconds}") long maximumRetryDelaySeconds
    ) {
        this.messageRepository = messageRepository;
        this.maximumRetryDelaySeconds = maximumRetryDelaySeconds;
    }

    /**
     * Method responsible for updating/deleting messages post processing dependent upon their status.
     * If a message is COMPLETED then it will be deleted from the queue, otherwise the Message in the
     * database will updated with the status and timestamp and its count will be incremented.
     *
     * @param message       The message that has been processed
     * @param messageStatus The status of the message
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void processStatusForMessage(Message message, MessageStatus messageStatus) {
        if (messageStatus == MessageStatus.COMPLETED) {
            messageRepository.delete(message);
        } else {
            updateMessageWithStatusAndIncrementCount(message, messageStatus);
        }
    }

    /**
     * Will set all the given messages to an ERROR status and update delivery counts
     * and timestamps accordingly.
     *
     * @param messages The messages to update
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateMessagesToErrorAndIncrementCount(List<Message> messages) {
        messages.forEach(message -> updateMessageWithStatusAndIncrementCount(message, ERROR));
    }

    private void updateMessageWithStatusAndIncrementCount(Message message, MessageStatus status) {
        int initialDeliveryCount = message.getDeliveryCount();
        LocalDateTime newTimestamp = LocalDateTime.now().plusSeconds(getRetryDelayInSeconds(initialDeliveryCount));

        message.setDeliveryCount(initialDeliveryCount + 1);
        message.setProcessAfter(newTimestamp);
        message.setStatus(status);
        messageRepository.save(message);
    }

    private long getRetryDelayInSeconds(int deliveryCount) {
        int exponent = Math.min(deliveryCount, MAX_EXPONENT_SIZE); // we overflow the size of a long if the exponent is too large
        long delay = ((long) Math.pow(2, exponent)) * THIRTY_SECONDS;
        return Math.min(delay, maximumRetryDelaySeconds);
    }
}
