package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.context.AdditionalPregnancyPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.CompletePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.RequestPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;

import java.util.Optional;

import static uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType.FIRST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType.REGULAR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType.RESTARTED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCalculationTestDataFactory.aFullPaymentCalculation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentResultTestDataFactory.aValidPaymentResult;

public class MessageContextTestDataFactory {

    public static DetermineEntitlementMessageContext aDetermineEntitlementMessageContext(PaymentCycle currentPaymentCycle,
                                                                                         PaymentCycle previousPaymentCycle,
                                                                                         Claim claim) {
        return DetermineEntitlementMessageContext.builder()
                .currentPaymentCycle(currentPaymentCycle)
                .previousPaymentCycle(previousPaymentCycle)
                .claim(claim)
                .build();
    }

    public static RequestPaymentMessageContext aValidRequestPaymentMessageContextForScheduledPayment(PaymentCycle paymentCycle, Claim claim) {
        return RequestPaymentMessageContext.builder()
                .paymentCycle(paymentCycle)
                .claim(claim)
                .paymentType(REGULAR_PAYMENT)
                .build();
    }

    public static RequestPaymentMessageContext aValidRequestPaymentMessageContextForFirstPayment(PaymentCycle paymentCycle, Claim claim) {
        return RequestPaymentMessageContext.builder()
                .paymentCycle(paymentCycle)
                .claim(claim)
                .paymentType(FIRST_PAYMENT)
                .build();
    }

    public static RequestPaymentMessageContext aValidRequestPaymentMessageContextForRestartedPayment(PaymentCycle paymentCycle, Claim claim) {
        return RequestPaymentMessageContext.builder()
                .paymentCycle(paymentCycle)
                .claim(claim)
                .paymentType(RESTARTED_PAYMENT)
                .build();
    }

    public static CompletePaymentMessageContext aValidCompletePaymentMessageContextForScheduledPayment(PaymentCycle paymentCycle, Claim claim) {
        return aValidCompletePaymentMessage(paymentCycle, claim, REGULAR_PAYMENT);
    }

    public static CompletePaymentMessageContext aValidCompletePaymentMessageContextForRestartedPayment(PaymentCycle paymentCycle, Claim claim) {
        return aValidCompletePaymentMessage(paymentCycle, claim, RESTARTED_PAYMENT);
    }

    public static CompletePaymentMessageContext aValidCompletePaymentMessageContextForFirstPayment(PaymentCycle paymentCycle, Claim claim) {
        return aValidCompletePaymentMessage(paymentCycle, claim, FIRST_PAYMENT);
    }

    private static CompletePaymentMessageContext aValidCompletePaymentMessage(PaymentCycle paymentCycle, Claim claim, PaymentType restartedPayment) {
        return CompletePaymentMessageContext.builder()
                .paymentCycle(paymentCycle)
                .claim(claim)
                .paymentType(restartedPayment)
                .paymentCalculation(aFullPaymentCalculation())
                .paymentResult(aValidPaymentResult())
                .build();
    }

    public static AdditionalPregnancyPaymentMessageContext aValidAdditionalPregnancyPaymentMessageContext(Claim claim, Optional<PaymentCycle> paymentCycle) {
        return AdditionalPregnancyPaymentMessageContext.builder()
                .claim(claim)
                .paymentCycle(paymentCycle)
                .build();
    }
}
