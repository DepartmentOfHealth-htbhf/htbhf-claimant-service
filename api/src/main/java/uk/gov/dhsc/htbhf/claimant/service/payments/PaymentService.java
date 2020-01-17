package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.model.card.CardBalanceResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;
import uk.gov.dhsc.htbhf.claimant.reporting.ReportPaymentMessageSender;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentRepository;
import uk.gov.dhsc.htbhf.claimant.service.CardClient;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.MakePaymentEvent;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.PAYMENT_AMOUNT;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.PAYMENT_REQUEST_REFERENCE;
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
     * Calculates the amount to pay, based on the total amount from the voucher entitlement, taking into account the balance currently on the card.
     *
     * @param paymentCycle  the current payment cycle, which should have a voucher entitlement.
     * @return the amount that should be deposited on the card account.
     */
    public PaymentCalculation calculatePaymentAmount(PaymentCycle paymentCycle) {
        CardBalanceResponse balance = cardClient.getBalance(paymentCycle.getClaim().getCardAccountId());
        return paymentCalculator.calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), balance.getAvailableBalanceInPence());
    }

    /**
     * Method used to store a failed payment to the database with the status of FAILURE. Will attempt to
     * retrieve the payment amount and reference from the failure event if available.
     *
     * @param paymentCycle  The payment cycle for the payment
     * @param failureEvent  The failure event.
     */
    public void saveFailedPayment(PaymentCycle paymentCycle, FailureEvent failureEvent) {
        String cardAccountId = paymentCycle.getClaim().getCardAccountId();
        try {
            Map<String, Object> eventMetadata = failureEvent.getEventMetadata();
            Integer amountToPayInPence = (Integer) eventMetadata.get(PAYMENT_AMOUNT.getKey());
            String paymentReference = (String) eventMetadata.get(PAYMENT_REQUEST_REFERENCE.getKey());
            String failureDetail = (String) eventMetadata.get(FailureEvent.EXCEPTION_DETAIL_KEY);
            Payment failedPayment = Payment.builder()
                    .cardAccountId(cardAccountId)
                    .claim(paymentCycle.getClaim())
                    .paymentAmountInPence(amountToPayInPence)
                    .paymentCycle(paymentCycle)
                    .requestReference(paymentReference)
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
     * Make a payment of the given amount to the given card account (extracted from the given payment cycle).
     *
     * @param paymentCycle         the cycle the payment is for
     * @param paymentAmountInPence the amount to pay, in pence.
     * @param paymentAction        the payment action, used for reporting.
     * @return PaymentResult       containing the request and response ids.
     */
    public PaymentResult makePayment(PaymentCycle paymentCycle, int paymentAmountInPence, PaymentAction paymentAction) {
        String requestReference = UUID.randomUUID().toString();
        String cardAccountId = paymentCycle.getClaim().getCardAccountId();
        try {
            DepositFundsResponse depositFundsResponse = depositFundsToCard(cardAccountId, requestReference, paymentAmountInPence);
            eventAuditor.auditMakePayment(paymentCycle, paymentAmountInPence, requestReference, depositFundsResponse.getReferenceId());
            reportPaymentMessageSender.sendReportPaymentMessage(paymentCycle.getClaim(), paymentCycle, paymentAction);
            return PaymentResult.builder()
                    .requestReference(requestReference)
                    .responseReference(depositFundsResponse.getReferenceId())
                    .paymentTimestamp(LocalDateTime.now())
                    .build();
        } catch (RuntimeException e) {
            String failureMessage = String.format("Payment failed for cardAccountId %s, claim %s, paymentCycle %s, exception is: %s",
                    cardAccountId, paymentCycle.getClaim().getId(), paymentCycle.getId(), e.getMessage());
            MakePaymentEvent failedEvent = buildFailedMakePaymentEvent(paymentCycle, paymentAmountInPence, requestReference);
            throw new EventFailedException(failedEvent, e, failureMessage);
        }
    }

    /**
     * Completes a payment by updating the payment cycle with a newly created a {@link Payment} entity.
     *
     * @param paymentCycle the current payment cycle
     * @param paymentCalculation the payment calculation that determined how much was paid
     * @param paymentResult the result of the payment
     */
    public void completePayment(PaymentCycle paymentCycle, PaymentCalculation paymentCalculation, PaymentResult paymentResult) {
        Payment payment = Payment.builder()
                .cardAccountId(paymentCycle.getClaim().getCardAccountId())
                .claim(paymentCycle.getClaim())
                .paymentAmountInPence(paymentCalculation.getPaymentAmount())
                .paymentCycle(paymentCycle)
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentTimestamp(paymentResult.getPaymentTimestamp())
                .requestReference(paymentResult.getRequestReference())
                .responseReference(paymentResult.getResponseReference())
                .build();
        paymentCycleService.updatePaymentCycleStatus(paymentCycle, paymentCalculation.getPaymentCycleStatus());
        paymentRepository.save(payment);
    }

    private MakePaymentEvent buildFailedMakePaymentEvent(PaymentCycle paymentCycle, int paymentAmountInPence, String requestReference) {
        return MakePaymentEvent.builder()
                .claimId(paymentCycle.getClaim().getId())
                .paymentAmountInPence(paymentAmountInPence)
                .requestReference(requestReference)
                .entitlementAmountInPence(paymentCycle.getTotalEntitlementAmountInPence())
                .build();
    }

    private DepositFundsResponse depositFundsToCard(String cardAccountId, String requestReference, int amountInPence) {
        DepositFundsRequest depositRequest = DepositFundsRequest.builder()
                .reference(requestReference)
                .amountInPence(amountInPence)
                .build();
        return cardClient.depositFundsToCard(cardAccountId, depositRequest);
    }
}
