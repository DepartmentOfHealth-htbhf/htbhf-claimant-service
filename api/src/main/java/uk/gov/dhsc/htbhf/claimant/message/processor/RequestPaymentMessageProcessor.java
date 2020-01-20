package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.RequestPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.CompletePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.BALANCE_TOO_HIGH_FOR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.COMPLETE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.INITIAL_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.SCHEDULED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation.aFullPaymentCalculationWithZeroBalance;

/**
 * Processes REQUEST_PAYMENT messages by calling the PaymentService.
 */
@Component
@AllArgsConstructor
@Slf4j
public class RequestPaymentMessageProcessor implements MessageTypeProcessor {

    private PaymentService paymentService;
    private MessageContextLoader messageContextLoader;
    private MessageQueueClient messageQueueClient;
    private PaymentCycleService paymentCycleService;
    private EventAuditor eventAuditor;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        RequestPaymentMessageContext messageContext = messageContextLoader.loadRequestPaymentMessageContext(message);
        PaymentCycle paymentCycle = messageContext.getPaymentCycle();
        PaymentType paymentType = messageContext.getPaymentType();

        // TODO AFHS-788 Handle payments with balance check overrides.
        if (paymentType == PaymentType.FIRST_PAYMENT) {
            PaymentCalculation paymentCalculation = aFullPaymentCalculationWithZeroBalance(paymentCycle.getTotalEntitlementAmountInPence());
            makePayment(paymentCycle, INITIAL_PAYMENT, paymentCalculation, paymentType);
        } else {
            PaymentCalculation paymentCalculation = paymentService.calculatePaymentAmount(paymentCycle);
            if (paymentCalculation.getPaymentCycleStatus() == BALANCE_TOO_HIGH_FOR_PAYMENT) {
                paymentCycleService.updatePaymentCycleFromCalculation(paymentCycle, paymentCalculation);
                log.debug("No payment will be made as the existing balance on the card is too high for PaymentCycle {}", paymentCycle.getId());
                eventAuditor.auditBalanceTooHighForPayment(paymentCycle);
                return COMPLETED;
            }
            makePayment(paymentCycle, SCHEDULED_PAYMENT, paymentCalculation, paymentType);
        }

        return COMPLETED;
    }

    private void makePayment(PaymentCycle paymentCycle, PaymentAction paymentAction, PaymentCalculation paymentCalculation, PaymentType paymentType) {
        PaymentResult paymentResult = paymentService.makePayment(paymentCycle, paymentCalculation.getPaymentAmount(), paymentAction);
        CompletePaymentMessagePayload payload = CompletePaymentMessagePayload.builder()
                .paymentType(paymentType)
                .paymentResult(paymentResult)
                .paymentCalculation(paymentCalculation)
                .paymentCycleId(paymentCycle.getId())
                .claimId(paymentCycle.getClaim().getId())
                .build();
        messageQueueClient.sendMessage(payload, COMPLETE_PAYMENT);
    }

    @Override
    public MessageType supportsMessageType() {
        return REQUEST_PAYMENT;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void processFailedMessage(Message message, FailureEvent failureEvent) {
        RequestPaymentMessageContext messageContext = messageContextLoader.loadRequestPaymentMessageContext(message);
        paymentService.saveFailedPayment(messageContext.getPaymentCycle(), failureEvent);
    }
}
