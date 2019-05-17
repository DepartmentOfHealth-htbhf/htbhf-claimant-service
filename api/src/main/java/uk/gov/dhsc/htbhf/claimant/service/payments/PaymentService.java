package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentRepository;
import uk.gov.dhsc.htbhf.claimant.service.CardClient;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

    private MessageQueueClient messageQueueClient;
    private CardClient cardClient;
    private PaymentRepository paymentRepository;
    private EventAuditor eventAuditor;

    public void createMakePaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }

    public Payment makeFirstPayment(PaymentCycle paymentCycle, String cardAccountId) {
        Payment payment = createPayment(paymentCycle, cardAccountId, paymentCycle.getTotalEntitlementAmountInPence());
        DepositFundsResponse response = depositFundsToCard(payment);
        updateAndSavePayment(payment, response.getReferenceId());
        eventAuditor.auditMakePayment(paymentCycle.getClaim().getId(), payment.getId(), response.getReferenceId());
        return payment;
    }

    public Payment makePayment(PaymentCycle paymentCycle, String cardAccountId) {
        Integer amountToPay = checkBalanceAndCalculatePaymentAmount(paymentCycle);
        // TODO: HTBHF-1267: If payment amount < minimum payment amount then don't make a payment (might need a new payment status)
        Payment payment = createPayment(paymentCycle, cardAccountId, amountToPay);
        DepositFundsResponse response = depositFundsToCard(payment);
        updateAndSavePayment(payment, response.getReferenceId());
        eventAuditor.auditMakePayment(paymentCycle.getClaim().getId(), payment.getId(), response.getReferenceId());
        return payment;
    }

    private Payment createPayment(PaymentCycle paymentCycle, String cardAccountId, Integer amountToPay) {
        return Payment.builder()
                    .cardAccountId(cardAccountId)
                    .claim(paymentCycle.getClaim())
                    .paymentAmountInPence(amountToPay)
                    .paymentCycle(paymentCycle)
                    .build();
    }

    private Integer checkBalanceAndCalculatePaymentAmount(PaymentCycle paymentCycle) {
        // TODO: HTBHF-1267: Check balance against card provider, update paymentCycle with balance and timestamp
        // TODO: HTBHF-1267: reduce amount paid if it would put the card over the max allowed balance
        return paymentCycle.getTotalEntitlementAmountInPence();
    }

    private DepositFundsResponse depositFundsToCard(Payment payment) {
        DepositFundsRequest depositRequest = DepositFundsRequest.builder()
                .reference(payment.getId().toString())
                .amountInPence(payment.getPaymentAmountInPence())
                .build();
        return cardClient.depositFundsToCard(payment.getCardAccountId(), depositRequest);
    }

    private void updateAndSavePayment(Payment payment, String referenceId) {
        payment.setPaymentReference(referenceId);
        payment.setPaymentTimestamp(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
    }
}
