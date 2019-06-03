package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.logging.event.Event;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

public class FailureEventTestDataFactory {

    public static FailureEvent aFailureEventWithEvent(Event event) {
        return FailureEvent.builder()
                .failedEvent(event)
                .failureDescription("my failure message")
                .exception(new RuntimeException("test exception"))
                .build();
    }
}
