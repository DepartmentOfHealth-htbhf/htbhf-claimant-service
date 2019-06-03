package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
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
import uk.gov.dhsc.htbhf.claimant.service.audit.MakePaymentEvent;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDateTime;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.PAYMENT_AMOUNT;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.PAYMENT_REFERENCE;
import static uk.gov.dhsc.htbhf.logging.ExceptionDetailGenerator.constructExceptionDetail;

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
     * Method used to store a failed payment to the database with the status of FAILURE. Will attempt to
     * retrieve the payment amount and reference from the failure event if available.
     *
     * @param paymentCycle  The payment cycle for the payment
     * @param cardAccountId The card account id
     * @param failureEvent  The failure event.
     */
    public void saveFailedPayment(PaymentCycle paymentCycle, String cardAccountId, FailureEvent failureEvent) {
        try {
            Map<String, Object> eventMetadata = failureEvent.getEventMetadata();
            Integer amountToPayInPence = (Integer) eventMetadata.get(PAYMENT_AMOUNT.getKey());
            String paymentReference = (String) eventMetadata.get(PAYMENT_REFERENCE.getKey());
            Payment failedPayment = Payment.builder()
                    .cardAccountId(cardAccountId)
                    .claim(paymentCycle.getClaim())
                    .paymentAmountInPence(amountToPayInPence)
                    .paymentCycle(paymentCycle)
                    .paymentReference(paymentReference)
                    .paymentStatus(PaymentStatus.FAILURE)
                    .build();

            paymentRepository.save(failedPayment);
        } catch (Exception e) {
            log.error("Unexpected exception caught saving a failed payment for paymentCycle: {}, cardAccountId: {}, failureEvent: {}, exception detail: {}",
                    paymentCycle.getId(), cardAccountId, failureEvent, constructExceptionDetail(e), e);
        }
    }

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
     * <p>
     * Note that the PMD warning is suppressed so that we can use the values of Payment and
     * DepositFundsResponse in the construction of the failed payment event if they have been set.
     * </p>
     *
     * @param paymentCycle  The new {@link PaymentCycle} associated with the new claim
     * @param cardAccountId The new card id to make to payment to
     * @return The {@link Payment} entity relevant to this process.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public Payment makeFirstPayment(PaymentCycle paymentCycle, String cardAccountId) {
        Payment payment = null;
        DepositFundsResponse depositFundsResponse = null;
        try {
            payment = createPayment(paymentCycle, cardAccountId, paymentCycle.getTotalEntitlementAmountInPence());
            depositFundsResponse = depositFundsToCard(payment);
            updatePayment(payment, depositFundsResponse.getReferenceId());
            updatePaymentCycle(paymentCycle, PaymentCycleStatus.FULL_PAYMENT_MADE);
            eventAuditor.auditMakePayment(paymentCycle, payment, depositFundsResponse);
        } catch (RuntimeException e) {
            String failureMessage = String.format("First payment failed for cardAccountId %s, claim %s, paymentCycle %s, exception is: %s",
                    cardAccountId, paymentCycle.getClaim().getId(), paymentCycle.getId(), e.getMessage());
            MakePaymentEvent failedEvent = buildFailedMakePaymentEvent(paymentCycle, payment, depositFundsResponse);
            throw new EventFailedException(failedEvent, e, failureMessage);
        }
        return payment;
    }

    /**
     * Calculate and (if appropriate) make the required payment for the given {@link PaymentCycle}.
     * <p>
     * Note that the PMD warning is suppressed so that we can use the values of Payment and
     * DepositFundsResponse in the construction of the failed payment event if they have been set.
     * </p>
     *
     * @param paymentCycle  The {@link PaymentCycle} to make the payment for
     * @param cardAccountId The card id to make the payment to.
     * @return The {@link Payment} entity relevant to this process.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public Payment makePayment(PaymentCycle paymentCycle, String cardAccountId) {
        Payment payment = null;
        DepositFundsResponse depositFundsResponse = null;
        try {
            CardBalanceResponse balance = cardClient.getBalance(cardAccountId);
            PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(),
                    balance.getAvailableBalanceInPence());
            updatePaymentCycle(paymentCycle, paymentCalculation, balance.getAvailableBalanceInPence());
            if (paymentCalculation.getPaymentAmount() == 0) {
                eventAuditor.auditBalanceTooHighForPayment(paymentCycle);
                log.info("No payment will be made as the existing balance on the card is too high");
                return null;
            }
            payment = createPayment(paymentCycle, cardAccountId, paymentCalculation.getPaymentAmount());
            depositFundsResponse = depositFundsToCard(payment);
            updatePayment(payment, depositFundsResponse.getReferenceId());
            eventAuditor.auditMakePayment(paymentCycle, payment, depositFundsResponse);
        } catch (RuntimeException e) {
            String failureMessage = String.format("Payment failed for cardAccountId %s, claim %s, paymentCycle %s, exception is: %s",
                    cardAccountId, paymentCycle.getClaim().getId(), paymentCycle.getId(), e.getMessage());
            MakePaymentEvent failedEvent = buildFailedMakePaymentEvent(paymentCycle, payment, depositFundsResponse);
            throw new EventFailedException(failedEvent, e, failureMessage);
        }
        return payment;
    }

    @SuppressWarnings("PMD.NullAssignment")
    private MakePaymentEvent buildFailedMakePaymentEvent(PaymentCycle paymentCycle, Payment payment, DepositFundsResponse depositFundsResponse) {
        return MakePaymentEvent.builder()
                .claimId(paymentCycle.getClaim().getId())
                .entitlementAmountInPence(paymentCycle.getTotalEntitlementAmountInPence())
                .paymentAmountInPence((payment == null) ? null : payment.getPaymentAmountInPence())
                .paymentId((payment == null) ? null : payment.getId())
                .reference((depositFundsResponse == null) ? null : depositFundsResponse.getReferenceId())
                .build();
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
