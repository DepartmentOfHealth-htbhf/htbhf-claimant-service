package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
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
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimAuditor;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

    private MessageQueueClient messageQueueClient;
    private CardClient cardClient;
    private PaymentRepository paymentRepository;
    private ClaimAuditor claimAuditor;

    public void createMakePaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }

    public Payment makePayment(PaymentCycle paymentCycle, String cardAccountId) {
        Claim claim = paymentCycle.getClaim();
        Integer amountToPay = paymentCycle.getTotalEntitlementAmountInPence();
        // TODO: HTBHF-1267: Check balance against card provider, update paymentCycle with balance and timestamp
        // TODO: HTBHF-1267: reduce amount paid if it would put the card over the max allowed balance
        Payment paymentRecord = Payment.builder()
                .cardAccountId(cardAccountId)
                .claim(claim)
                .paymentAmountInPence(amountToPay)
                .paymentCycle(paymentCycle)
                .build();
        DepositFundsResponse response = depositFundsToCard(paymentRecord);
        updateAndSavePayment(paymentRecord, response);
        claimAuditor.auditMakePayment(claim.getId(), paymentRecord.getId(), response.getReferenceId());
        return paymentRecord;
    }

    private DepositFundsResponse depositFundsToCard(Payment payment) {
        DepositFundsRequest depositRequest = DepositFundsRequest.builder()
                .reference(payment.getId().toString())
                .amountInPence(payment.getPaymentAmountInPence())
                .build();
        return cardClient.depositFundsToCard(payment.getCardAccountId(), depositRequest);
    }

    private void updateAndSavePayment(Payment payment, DepositFundsResponse response) {
        payment.setPaymentReference(response.getReferenceId());
        payment.setPaymentTimestamp(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
    }
}
