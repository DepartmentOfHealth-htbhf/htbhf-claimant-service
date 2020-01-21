package uk.gov.dhsc.htbhf.claimant.factory;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus.SUCCESS;
import static uk.gov.dhsc.htbhf.claimant.factory.PaymentFactory.createFailedPayment;
import static uk.gov.dhsc.htbhf.claimant.factory.PaymentFactory.createSuccessfulPayment;
import static uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation.aFullPaymentCalculationWithZeroBalance;
import static uk.gov.dhsc.htbhf.claimant.testsupport.FailureEventTestDataFactory.aFailedMakePaymentEvent;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentResultTestDataFactory.aValidPaymentResult;

class PaymentFactoryTest {

    @Test
    void shouldCreateFailedPayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        int amountToPay = 100;
        String requestReference = UUID.randomUUID().toString();
        FailureEvent failureEvent = aFailedMakePaymentEvent(paymentCycle, amountToPay, requestReference);


        Payment payment = createFailedPayment(paymentCycle, failureEvent);

        assertThat(payment.getCardAccountId()).isEqualTo(paymentCycle.getClaim().getCardAccountId());
        assertThat(payment.getClaim()).isEqualTo(paymentCycle.getClaim());
        assertThat(payment.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILURE);
        assertThat(payment.getPaymentTimestamp()).isEqualTo(failureEvent.getTimestamp());
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(amountToPay);
        assertThat(payment.getRequestReference()).isEqualTo(requestReference);
        assertThat(payment.getFailureDetail()).isEqualTo(failureEvent.getEventMetadata().get(FailureEvent.EXCEPTION_DETAIL_KEY));
    }

    @Test
    void shouldCreateCompletedPayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        int paymentAmount = 100;
        PaymentCalculation paymentCalculation = aFullPaymentCalculationWithZeroBalance(paymentAmount);
        PaymentResult paymentResult = aValidPaymentResult();

        Payment payment = createSuccessfulPayment(paymentCycle, paymentCalculation, paymentResult);

        assertThat(payment.getCardAccountId()).isEqualTo(paymentCycle.getClaim().getCardAccountId());
        assertThat(payment.getClaim()).isEqualTo(paymentCycle.getClaim());
        assertThat(payment.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(payment.getPaymentStatus()).isEqualTo(SUCCESS);
        assertThat(payment.getPaymentTimestamp()).isNotNull();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(paymentCalculation.getPaymentAmount());
        assertThat(payment.getRequestReference()).isEqualTo(paymentResult.getRequestReference());
        assertThat(payment.getResponseReference()).isEqualTo(paymentResult.getResponseReference());
    }
}
