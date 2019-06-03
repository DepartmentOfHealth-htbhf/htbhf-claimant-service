package uk.gov.dhsc.htbhf.claimant.exception;

import uk.gov.dhsc.htbhf.logging.event.Event;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

/**
 * RuntimeException that can be thrown when an event has failed and we wish to pass the detail
 * of the event back up the stack for further processing.
 */
public class EventFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final FailureEvent failureEvent;

    public EventFailedException(Event failedEvent, Exception cause, String message) {
        super(message, cause);
        this.failureEvent = FailureEvent.builder()
                .failedEvent(failedEvent)
                .failureDescription(message)
                .exception(cause)
                .build();
    }

    public FailureEvent getFailureEvent() {
        return failureEvent;
    }
}
