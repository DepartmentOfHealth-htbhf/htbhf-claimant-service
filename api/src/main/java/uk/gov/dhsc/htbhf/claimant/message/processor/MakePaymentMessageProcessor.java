package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MakePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDate;
import java.util.Optional;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildChildTurnsFourNotificationEmailPayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildPaymentNotificationEmailPayload;
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
    private PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;
    private MessageQueueClient messageQueueClient;
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        MakePaymentMessageContext messageContext = messageContextLoader.loadMakePaymentContext(message);
        PaymentCycle paymentCycle = messageContext.getPaymentCycle();
        paymentService.makePaymentForCycle(paymentCycle, messageContext.getCardAccountId());
        EmailMessagePayload messagePayload = buildPaymentNotificationEmailPayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
        NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment =
                childDateOfBirthCalculator.getChildrenDateOfBirthSummaryAffectingNextPayment(paymentCycle);
        if (dateOfBirthSummaryAffectingNextPayment.hasChildrenTurningFour()) {
            sendChildTurnsFourEmail(paymentCycle, dateOfBirthSummaryAffectingNextPayment);
        }
        return COMPLETED;
    }

    private void sendChildTurnsFourEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        PaymentCycleVoucherEntitlement entitlement = determineEntitlementForNextCycle(paymentCycle);
        boolean multipleChildrenTurningFourInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningFour();
        EmailMessagePayload messagePayload = buildChildTurnsFourNotificationEmailPayload(
                paymentCycle,
                entitlement,
                multipleChildrenTurningFourInNextMonth);
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
    }

    private PaymentCycleVoucherEntitlement determineEntitlementForNextCycle(PaymentCycle currentPaymentCycle) {
        LocalDate nextCycleStartDate = currentPaymentCycle.getCycleEndDate().plusDays(1);
        return paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(currentPaymentCycle.getClaim().getClaimant().getExpectedDeliveryDate()),
                currentPaymentCycle.getChildrenDob(),
                nextCycleStartDate,
                currentPaymentCycle.getVoucherEntitlement());
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
