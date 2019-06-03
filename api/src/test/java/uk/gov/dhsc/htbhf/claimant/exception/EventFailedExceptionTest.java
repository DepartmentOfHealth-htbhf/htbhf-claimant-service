package uk.gov.dhsc.htbhf.claimant.exception;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewCardEvent;
import uk.gov.dhsc.htbhf.logging.event.CommonEventType;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.EXCEPTION_DETAIL_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILED_EVENT_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILURE_DESCRIPTION_KEY;

class EventFailedExceptionTest {

    @Test
    void shouldCreateFailureEventOnConstruction() {
        //Given
        UUID claimId = UUID.randomUUID();
        NewCardEvent event = NewCardEvent.builder().claimId(claimId).build();
        String failureMessage = "Something went badly wrong";
        RuntimeException testException = new RuntimeException("test exception");

        //When
        EventFailedException exception = new EventFailedException(event, testException, failureMessage);

        //Then
        assertThat(exception).hasMessage(failureMessage);
        assertThat(exception).hasCause(testException);
        FailureEvent failureEvent = exception.getFailureEvent();
        assertThat(failureEvent.getEventType()).isEqualTo(CommonEventType.FAILURE);
        assertThat(failureEvent.getTimestamp()).isEqualTo(event.getTimestamp());
        Map<String, Object> metadata = failureEvent.getEventMetadata();
        assertThat(metadata).containsAllEntriesOf(event.getEventMetadata());
        assertThat(metadata.get(FAILED_EVENT_KEY)).isEqualTo(event.getEventType());
        assertThat(metadata.get(FAILURE_DESCRIPTION_KEY)).isEqualTo(failureMessage);
        String actualExceptionDetail = (String) metadata.get(EXCEPTION_DETAIL_KEY);
        assertThat(actualExceptionDetail).startsWith("test exception");
        assertThat(actualExceptionDetail).contains("EventFailedExceptionTest.shouldCreateFailureEventOnConstruction");
    }
}
