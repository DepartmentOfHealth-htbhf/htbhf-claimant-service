package uk.gov.dhsc.htbhf.claimant.service.audit;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.logging.event.CommonEventType;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.EMAIL_TEMPLATE_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.EMAIL_TYPE;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.EXCEPTION_DETAIL_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILED_EVENT_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILURE_DESCRIPTION_KEY;

public class FailedEventTestUtils {

    public static void verifySendEmailEventFailExceptionAndEventAreCorrect(Claim claim,
                                                                           NotificationClientException testException,
                                                                           EventFailedException exception,
                                                                           String templateId) {
        verifyCommonEventFailureDetail(exception, testException);
        Map<String, Object> metadata = exception.getFailureEvent().getEventMetadata();
        assertThat(metadata).contains(
                entry(CLAIM_ID.getKey(), claim.getId()),
                entry(FAILED_EVENT_KEY, ClaimEventType.FAILED_EMAIL),
                entry(EMAIL_TEMPLATE_ID.getKey(), templateId),
                entry(EMAIL_TYPE.getKey(), EmailType.NEW_CARD.name()));
    }

    //TODO MRS 2019-07-29: In next PR, refactor other instances of testing event failures to use this similar/common verification method
    private static void verifyCommonEventFailureDetail(EventFailedException exception, Exception failureCause) {
        String expectedFailureMessage = "Failed to send NEW_CARD email message, exception is: Test exception from message send";
        assertThat(exception).hasMessage(expectedFailureMessage);
        assertThat(exception).hasCause(failureCause);

        FailureEvent failureEvent = exception.getFailureEvent();
        assertThat(failureEvent.getEventType()).isEqualTo(CommonEventType.FAILURE);
        assertThat(failureEvent.getTimestamp()).isNotNull();

        Map<String, Object> metadata = failureEvent.getEventMetadata();
        assertThat(metadata).contains(entry(FAILURE_DESCRIPTION_KEY, expectedFailureMessage));

        String actualExceptionDetail = (String) metadata.get(EXCEPTION_DETAIL_KEY);
        assertThat(actualExceptionDetail).startsWith("Test exception from message send");
        assertThat(actualExceptionDetail).contains("EmailMessageProcessor.processMessage");
    }


}
