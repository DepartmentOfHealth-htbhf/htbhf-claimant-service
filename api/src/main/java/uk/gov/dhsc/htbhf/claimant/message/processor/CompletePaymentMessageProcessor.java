package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.communications.PaymentCycleNotificationHandler;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.CompletePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

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

    private PaymentCycleNotificationHandler paymentCycleNotificationHandler;
    private PaymentService paymentService;
    private MessageContextLoader messageContextLoader;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        CompletePaymentMessageContext messageContext = messageContextLoader.loadCompletePaymentMessageContext(message);
        paymentService.completePayment(messageContext.getPaymentCycle(), messageContext.getPaymentCalculation(), messageContext.getPaymentResult());
        sendNotificationEmail(messageContext);
        return COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return COMPLETE_PAYMENT;
    }

    private void sendNotificationEmail(CompletePaymentMessageContext messageContext) {
        if (messageContext.getPaymentType() == PaymentType.RESTARTED_PAYMENT) {
            paymentCycleNotificationHandler.sendNotificationEmailsForRestartedPayment(messageContext.getPaymentCycle());
        } else if (messageContext.getPaymentType() == PaymentType.REGULAR_PAYMENT) {
            paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(messageContext.getPaymentCycle());
        }
    }

}
