package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.COMPLETE_PAYMENT;

/**
 * Processes COMPLETE_PAYMENT messages by calling the PaymentService.
 */
@Component
@AllArgsConstructor
@Slf4j
public class CompletePaymentMessageProcessor implements MessageTypeProcessor {

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        // TODO AFHS-405 add processing logic when implementing
        return COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return COMPLETE_PAYMENT;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void processFailedMessage(Message message, FailureEvent failureEvent) {
        // TODO AFHS-405 add failure when implementing
    }

}
