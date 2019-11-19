package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.model.card.CardBalanceResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;
import uk.gov.dhsc.htbhf.claimant.reporting.ReportPaymentMessageSender;
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

    private CardClient cardClient;
    private PaymentRepository paymentRepository;
    private PaymentCycleService paymentCycleService;
    private EventAuditor eventAuditor;
    private PaymentCalculator paymentCalculator;
    private ReportPaymentMessageSender reportPaymentMessageSender;

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
            String failureDetail = (String) eventMetadata.get(FailureEvent.EXCEPTION_DETAIL_KEY);
            Payment failedPayment = Payment.builder()
                    .cardAccountId(cardAccountId)
                    .claim(paymentCycle.getClaim())
                    .paymentAmountInPence(amountToPayInPence)
                    .paymentCycle(paymentCycle)
                    .paymentReference(paymentReference)
                    .paymentStatus(PaymentStatus.FAILURE)
                    .failureDetail(failureDetail)
                    .paymentTimestamp(failureEvent.getTimestamp())
                    .build();

            paymentRepository.save(failedPayment);
        } catch (Exception e) {
            log.error("Unexpected exception caught saving a failed payment for paymentCycle: {}, cardAccountId: {}, failureEvent: {}, exception detail: {}",
                    paymentCycle.getId(), cardAccountId, failureEvent, constructExceptionDetail(e), e);
        }
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
    public Payment makeFirstPayment(PaymentCycle paymentCycle, String cardAccountId) {
        Payment payment = makePayment(
                paymentCycle,
                cardAccountId,
                paymentCycle.getTotalEntitlementAmountInPence(),
                paymentCycle.getTotalEntitlementAmountInPence()
        );
        paymentCycleService.updatePaymentCycle(paymentCycle, PaymentCycleStatus.FULL_PAYMENT_MADE);
        reportPaymentMessageSender.sendReportInitialPaymentMessage(paymentCycle.getClaim(), paymentCycle);
        return payment;
    }

    /**
     * Make a payment onto a card without doing a balance check. Used for interim payments.
     *
     * @param paymentCycle  the current payment cycle
     * @param cardAccountId the id of the card account
     * @param amountInPence the amount to deposit to the card in pence
     * @return The {@link Payment} entity relevant to this process.
     */
    public Payment makeInterimPayment(PaymentCycle paymentCycle, String cardAccountId, int amountInPence) {
        return makePayment(paymentCycle, cardAccountId, amountInPence, null);
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
    public Payment makePaymentForCycle(PaymentCycle paymentCycle, String cardAccountId) {
        Payment payment = null;
        DepositFundsResponse depositFundsResponse = null;
        try {
            CardBalanceResponse balance = cardClient.getBalance(cardAccountId);
            PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(),
                    balance.getAvailableBalanceInPence());
            paymentCycleService.updatePaymentCycle(paymentCycle, paymentCalculation.getPaymentCycleStatus(), balance.getAvailableBalanceInPence());
            if (paymentCalculation.getPaymentAmount() == 0) {
                eventAuditor.auditBalanceTooHighForPayment(paymentCycle);
                log.debug("No payment will be made as the existing balance on the card is too high for PaymentCycle {}", paymentCycle.getId());
                return null;
            }
            payment = createPayment(paymentCycle, cardAccountId, paymentCalculation.getPaymentAmount());
            depositFundsResponse = depositFundsToCard(payment);
            updatePayment(payment, depositFundsResponse.getReferenceId());
            eventAuditor.auditMakePayment(paymentCycle, payment, depositFundsResponse);
            reportPaymentMessageSender.sendReportScheduledPayment(paymentCycle.getClaim(), paymentCycle);
        } catch (RuntimeException e) {
            String failureMessage = String.format("Payment failed for cardAccountId %s, claim %s, paymentCycle %s, exception is: %s",
                    cardAccountId, paymentCycle.getClaim().getId(), paymentCycle.getId(), e.getMessage());
            MakePaymentEvent failedEvent
                    = buildFailedMakePaymentEvent(paymentCycle.getClaim(), paymentCycle.getTotalEntitlementAmountInPence(), payment, depositFundsResponse);
            throw new EventFailedException(failedEvent, e, failureMessage);
        }
        return payment;
    }

    private Payment makePayment(PaymentCycle paymentCycle, String cardAccountId, int amountInPence, Integer entitlementAmountInPence) {
        Payment payment = null;
        DepositFundsResponse depositFundsResponse = null;
        try {
            payment = createPayment(paymentCycle, cardAccountId, amountInPence);
            depositFundsResponse = depositFundsToCard(payment);
            updatePayment(payment, depositFundsResponse.getReferenceId());
            eventAuditor.auditMakePayment(paymentCycle, payment, depositFundsResponse);
        } catch (RuntimeException e) {
            String failureMessage = String.format("Payment failed for cardAccountId %s, claim %s, paymentCycle %s, exception is: %s",
                    cardAccountId, paymentCycle.getClaim().getId(), paymentCycle.getId(), e.getMessage());
            MakePaymentEvent failedEvent = buildFailedMakePaymentEvent(paymentCycle.getClaim(), entitlementAmountInPence, payment, depositFundsResponse);
            throw new EventFailedException(failedEvent, e, failureMessage);
        }
        return payment;
    }

    @SuppressWarnings("PMD.NullAssignment")
    private MakePaymentEvent buildFailedMakePaymentEvent(Claim claim, Integer entitlementInPence, Payment payment, DepositFundsResponse depositFundsResponse) {
        return MakePaymentEvent.builder()
                .claimId(claim.getId())
                .entitlementAmountInPence(entitlementInPence)
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
