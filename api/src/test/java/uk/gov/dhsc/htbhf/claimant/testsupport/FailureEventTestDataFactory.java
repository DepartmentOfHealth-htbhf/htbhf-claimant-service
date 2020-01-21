package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.service.audit.MakePaymentEvent;
import uk.gov.dhsc.htbhf.logging.event.Event;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION;

public class FailureEventTestDataFactory {

    public static FailureEvent aFailureEventWithEvent(Event event) {
        return FailureEvent.builder()
                .failedEvent(event)
                .failureDescription("my failure message")
                .exception(TEST_EXCEPTION)
                .build();
    }

    public static FailureEvent aFailedMakePaymentEvent(PaymentCycle paymentCycle, int amountToPay, String requestReference) {
        MakePaymentEvent event = MakePaymentEvent.builder()
                .claimId(paymentCycle.getClaim().getId())
                .paymentAmountInPence(amountToPay)
                .requestReference(requestReference)
                .build();
        return aFailureEventWithEvent(event);
    }
}
