package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
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

    /**
     * Build and send a MAKE_PAYMENT message for the given {@link PaymentCycle}.
     *
     * @param paymentCycle The payment cycle to make a payment for.
     */
    public void createMakePaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }

    /**
     * Make the first payment onto a new card as a part of a successful application process.
     *
     * @param paymentCycle  The new {@link PaymentCycle} associated with the new claim
     * @param cardAccountId The new card id to make to payment to
     * @return The {@link Payment} entity relevant to this process.
     */
    public Payment makeFirstPayment(PaymentCycle paymentCycle, String cardAccountId) {
        Payment payment = createPayment(paymentCycle, cardAccountId, paymentCycle.getTotalEntitlementAmountInPence());
        DepositFundsResponse depositFundsResponse = depositFundsToCard(payment);
        updatePayment(payment, depositFundsResponse.getReferenceId());
        updatePaymentCycle(paymentCycle, PaymentCycleStatus.FULL_PAYMENT_MADE);
        eventAuditor.auditMakePayment(paymentCycle, payment, depositFundsResponse);
        return payment;
    }

    /**
     * Calculate and (if appropriate) make the required payment for the given {@link PaymentCycle}.
     *
     * @param paymentCycle  The {@link PaymentCycle} to make the payment for
     * @param cardAccountId The card id to make the payment to.
     * @return The {@link Payment} entity relevant to this process.
     */
    public Payment makePayment(PaymentCycle paymentCycle, String cardAccountId) {
        CardBalanceResponse balance = cardClient.getBalance(cardAccountId);
        PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(),
                balance.getAvailableBalanceInPence());
        updatePaymentCycle(paymentCycle, paymentCalculation, balance.getAvailableBalanceInPence());
        if (paymentCalculation.getPaymentAmount() == 0) {
            eventAuditor.auditBalanceTooHighForPayment(paymentCycle);
            log.info("No payment will be made as the existing balance on the card is too high");
            return null;
        }
        Payment payment = createPayment(paymentCycle, cardAccountId, paymentCalculation.getPaymentAmount());
        DepositFundsResponse depositFundsResponse = depositFundsToCard(payment);
        updatePayment(payment, depositFundsResponse.getReferenceId());
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

    private void updatePaymentCycle(PaymentCycle paymentCycle, PaymentCalculation paymentCalculation, int cardBalanceInPence) {
        paymentCycle.setCardBalanceInPence(cardBalanceInPence);
        paymentCycle.setCardBalanceTimestamp(LocalDateTime.now());
        updatePaymentCycle(paymentCycle, paymentCalculation.getPaymentCycleStatus());
    }

    private void updatePaymentCycle(PaymentCycle paymentCycle, PaymentCycleStatus paymentCycleStatus) {
        paymentCycle.setPaymentCycleStatus(paymentCycleStatus);
        paymentCycleService.savePaymentCycle(paymentCycle);
    }

    private DepositFundsResponse depositFundsToCard(Payment payment) {
        DepositFundsRequest depositRequest = DepositFundsRequest.builder()
                .reference(payment.getId().toString())
                .amountInPence(payment.getPaymentAmountInPence())
                .build();
        return cardClient.depositFundsToCard(payment.getCardAccountId(), depositRequest);
    }

    private void updatePayment(Payment payment, String referenceId) {
        payment.setPaymentReference(referenceId);
        payment.setPaymentTimestamp(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
    }
}
