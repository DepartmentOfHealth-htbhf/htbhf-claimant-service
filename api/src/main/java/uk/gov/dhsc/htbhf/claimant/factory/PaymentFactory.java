package uk.gov.dhsc.htbhf.claimant.factory;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.PAYMENT_AMOUNT;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.PAYMENT_REQUEST_REFERENCE;

public class PaymentFactory {

    /**
     * Creates a failed {@link Payment} with a payment status of {@link PaymentStatus#FAILURE}. To be used when an error occurred whilst making a payment.
     * @param paymentCycle Payment cycle linked to the payment
     * @param failureEvent failure event containing payment amount, request reference and exception detail.
     * @return new payment
     */
    public static Payment createFailedPayment(PaymentCycle paymentCycle, FailureEvent failureEvent) {
        Map<String, Object> eventMetadata = failureEvent.getEventMetadata();
        Integer amountToPayInPence = (Integer) eventMetadata.get(PAYMENT_AMOUNT.getKey());
        String paymentReference = (String) eventMetadata.get(PAYMENT_REQUEST_REFERENCE.getKey());
        String failureDetail = (String) eventMetadata.get(FailureEvent.EXCEPTION_DETAIL_KEY);
        Claim claim = paymentCycle.getClaim();
        return Payment.builder()
                .cardAccountId(claim.getCardAccountId())
                .claim(claim)
                .paymentAmountInPence(amountToPayInPence)
                .paymentCycle(paymentCycle)
                .requestReference(paymentReference)
                .paymentStatus(PaymentStatus.FAILURE)
                .failureDetail(failureDetail)
                .paymentTimestamp(failureEvent.getTimestamp())
                .build();
    }

    /**
     * Creates a new payment with a status of {@link PaymentStatus#SUCCESS}. To be used when making a payment was successful.
     * @param paymentCycle Payment cycle linked to the payment
     * @param paymentCalculation payment calculation used when making the payment
     * @param paymentResult the result of the payment
     * @return new payment
     */
    public static Payment createSuccessfulPayment(PaymentCycle paymentCycle, PaymentCalculation paymentCalculation, PaymentResult paymentResult) {
        return Payment.builder()
                .cardAccountId(paymentCycle.getClaim().getCardAccountId())
                .claim(paymentCycle.getClaim())
                .paymentAmountInPence(paymentCalculation.getPaymentAmount())
                .paymentCycle(paymentCycle)
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentTimestamp(paymentResult.getPaymentTimestamp())
                .requestReference(paymentResult.getRequestReference())
                .responseReference(paymentResult.getResponseReference())
                .build();
    }
}
