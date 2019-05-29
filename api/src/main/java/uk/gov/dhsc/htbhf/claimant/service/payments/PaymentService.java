package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.card.CardBalanceResponse;
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
    private PaymentCycleService paymentCycleService;
    private EventAuditor eventAuditor;
    private PaymentCalculator paymentCalculator;

    public void createMakePaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }

    public Payment makeFirstPayment(PaymentCycle paymentCycle, String cardAccountId) {
        Payment payment = createPayment(paymentCycle, cardAccountId, paymentCycle.getTotalEntitlementAmountInPence());
        DepositFundsResponse depositFundsResponse = depositFundsToCard(payment);
        updateAndSavePayment(payment, depositFundsResponse.getReferenceId());
        eventAuditor.auditMakePayment(paymentCycle, payment, depositFundsResponse);
        return payment;
    }

    public Payment makePayment(PaymentCycle paymentCycle, String cardAccountId) {
        Integer amountToPay = checkBalanceAndCalculatePaymentAmount(paymentCycle, cardAccountId);
        if (amountToPay == 0) {
            eventAuditor.auditBalanceTooHighForPayment(
                    paymentCycle.getClaim().getId(),
                    paymentCycle.getTotalEntitlementAmountInPence(),
                    paymentCycle.getCardBalanceInPence());
            log.info("No payment will be made as the existing balance on the card is too high");
            return null;
        }
        Payment payment = createPayment(paymentCycle, cardAccountId, amountToPay);
        DepositFundsResponse depositFundsResponse = depositFundsToCard(payment);
        updateAndSavePayment(payment, depositFundsResponse.getReferenceId());
        eventAuditor.auditMakePayment(paymentCycle, payment, depositFundsResponse);
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

    private Integer checkBalanceAndCalculatePaymentAmount(PaymentCycle paymentCycle, String cardAccountId) {
        CardBalanceResponse balance = cardClient.getBalance(cardAccountId);
        paymentCycle.setCardBalanceInPence(balance.getAvailableBalanceInPence());
        paymentCycle.setCardBalanceTimestamp(LocalDateTime.now());
        PaymentCycleVoucherEntitlement voucherEntitlement = paymentCycle.getVoucherEntitlement();
        PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(voucherEntitlement, balance.getAvailableBalanceInPence());
        paymentCycle.setPaymentCycleStatus(paymentCalculation.getPaymentCycleStatus());
        paymentCycleService.savePaymentCycle(paymentCycle);
        return paymentCalculation.getPaymentAmount();
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
