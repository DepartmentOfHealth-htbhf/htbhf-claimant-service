package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.PayloadMapper;
import uk.gov.dhsc.htbhf.claimant.message.context.MakePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;

/**
 * Processes MAKE_PAYMENT messages by calling the PaymentService.
 */
@Component
@AllArgsConstructor
@Slf4j
public class MakePaymentMessageProcessor implements MessageTypeProcessor {

    private PaymentService paymentService;
    private MessageRepository messageRepository;
    private PayloadMapper payloadMapper;
    private MessageContextLoader messageContextLoader;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        MakePaymentMessagePayload payload = payloadMapper.getPayload(message, MakePaymentMessagePayload.class);
        MakePaymentMessageContext messageContext = messageContextLoader.loadContext(payload);
        paymentService.makePayment(messageContext.getPaymentCycle(), messageContext.getCardAccountId());
        messageRepository.delete(message);
        return COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return MAKE_PAYMENT;
    }

}
