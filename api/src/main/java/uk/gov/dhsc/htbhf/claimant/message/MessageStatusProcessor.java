package uk.gov.dhsc.htbhf.claimant.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.ERROR;

@RequiredArgsConstructor
@Component
public class MessageStatusProcessor {

    private final MessageRepository messageRepository;

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
        int updatedDeliveryCount = message.getDeliveryCount() + 1;
        message.setDeliveryCount(updatedDeliveryCount);
        message.setMessageTimestamp(LocalDateTime.now());
        message.setStatus(status);
        messageRepository.save(message);
    }
}
