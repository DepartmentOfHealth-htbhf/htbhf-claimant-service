package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.communications.PaymentCycleNotificationHandler;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MakePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

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
    private MessageContextLoader messageContextLoader;
    private PaymentCycleNotificationHandler paymentCycleNotificationHandler;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        MakePaymentMessageContext messageContext = messageContextLoader.loadMakePaymentContext(message);
        PaymentCycle paymentCycle = messageContext.getPaymentCycle();
        if (messageContext.getPaymentType() == PaymentType.FIRST_PAYMENT) {
            paymentService.makeFirstPayment(paymentCycle, messageContext.getCardAccountId());
        } else {
            paymentService.makePaymentForCycle(paymentCycle, messageContext.getCardAccountId());
            sendNotificationEmail(messageContext, paymentCycle);
        }

        return COMPLETED;
    }

    private void sendNotificationEmail(MakePaymentMessageContext messageContext, PaymentCycle paymentCycle) {
        if (messageContext.getPaymentType() == PaymentType.RESTARTED_PAYMENT) {
            paymentCycleNotificationHandler.sendNotificationEmailsForRestartedPayment(paymentCycle);
        } else {
            paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(paymentCycle);
        }
    }

    @Override
    public MessageType supportsMessageType() {
        return MAKE_PAYMENT;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void processFailedMessage(Message message, FailureEvent failureEvent) {
        MakePaymentMessageContext messageContext = messageContextLoader.loadMakePaymentContext(message);
        paymentService.saveFailedPayment(messageContext.getPaymentCycle(), messageContext.getCardAccountId(), failureEvent);
    }

}
