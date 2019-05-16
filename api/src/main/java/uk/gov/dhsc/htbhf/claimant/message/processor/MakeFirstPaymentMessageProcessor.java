package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MakePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;

/**
 * Responsible for processing MAKE_FIRST_PAYMENT messages by invoking the PaymentService.
 */
@Component
@AllArgsConstructor
@Slf4j
public class MakeFirstPaymentMessageProcessor implements MessageTypeProcessor {

    private PaymentService paymentService;
    private MessageRepository messageRepository;
    private MessageContextLoader messageContextLoader;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        MakePaymentMessageContext messageContext = messageContextLoader.loadMakePaymentContext(message);
        paymentService.makeFirstPayment(messageContext.getPaymentCycle(), messageContext.getCardAccountId());
        messageRepository.delete(message);
        return COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return MAKE_FIRST_PAYMENT;
    }

}
