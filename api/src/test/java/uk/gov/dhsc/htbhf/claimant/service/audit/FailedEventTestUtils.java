package uk.gov.dhsc.htbhf.claimant.service.audit;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;
import uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants;
import uk.gov.dhsc.htbhf.logging.event.CommonEventType;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION_MESSAGE;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.EXCEPTION_DETAIL_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILED_EVENT_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILURE_DESCRIPTION_KEY;

public class FailedEventTestUtils {

    public static void verifyFailedPaymentEventFailExceptionAndEventCorrect(PaymentCycle paymentCycle,
                                                                            RuntimeException testException,
                                                                            EventFailedException exception,
                                                                            Integer totalEntitlementAmountInPence,
                                                                            Integer paymentAmountInPence) {
        String expectedFailureMessage = String.format("Payment failed for cardAccountId %s, claim %s, paymentCycle %s, exception is: %s",
                TestConstants.CARD_ACCOUNT_ID, paymentCycle.getClaim().getId(), paymentCycle.getId(), TEST_EXCEPTION_MESSAGE);
        verifyCommonEventFailureDetail(exception, testException, expectedFailureMessage, paymentCycle.getClaim(), ClaimEventType.MAKE_PAYMENT);
        Map<String, Object> metadata = exception.getFailureEvent().getEventMetadata();
        assertExceptionDetailCorrect(metadata, TEST_EXCEPTION_MESSAGE, "PaymentService.depositFundsToCard");

        assertThat(metadata).contains(
                entry(CLAIM_ID.getKey(), paymentCycle.getClaim().getId()),
                entry(ENTITLEMENT_AMOUNT_IN_PENCE.getKey(), totalEntitlementAmountInPence),
                entry(FAILED_EVENT_KEY, ClaimEventType.MAKE_PAYMENT),
                entry(PAYMENT_AMOUNT.getKey(), paymentAmountInPence));
        assertThat(metadata.get(PAYMENT_ID.getKey())).isNotNull();
        assertThat(metadata.get(PAYMENT_REFERENCE.getKey())).isNull();

    }

    public static void verifySendEmailEventFailExceptionAndEventAreCorrect(Claim claim,
                                                                           NotificationClientException testException,
                                                                           EventFailedException exception,
                                                                           String templateId) {
        String expectedFailureMessage = "Failed to send INSTANT_SUCCESS email message, exception is: Test exception from message send";
        verifyCommonEventFailureDetail(exception, testException, expectedFailureMessage, claim, ClaimEventType.FAILED_EMAIL);
        Map<String, Object> metadata = exception.getFailureEvent().getEventMetadata();
        assertExceptionDetailCorrect(metadata, "Test exception from message send", "EmailMessageProcessor.processMessage");
        assertThat(metadata).contains(
                entry(TEMPLATE_ID.getKey(), templateId),
                entry(EMAIL_TYPE.getKey(), EmailType.INSTANT_SUCCESS.name()));
    }

    public static void verifySendLetterEventFailExceptionAndEventAreCorrect(Claim claim,
                                                                           NotificationClientException testException,
                                                                           EventFailedException exception,
                                                                           String templateId) {
        String expectedFailureMessage = "Failed to send UPDATE_YOUR_ADDRESS letter, exception is: Test exception from message send";
        verifyCommonEventFailureDetail(exception, testException, expectedFailureMessage, claim, ClaimEventType.FAILED_LETTER);
        Map<String, Object> metadata = exception.getFailureEvent().getEventMetadata();
        assertExceptionDetailCorrect(metadata, "Test exception from message send", "LetterMessageProcessor.processMessage");
        assertThat(metadata).contains(
                entry(TEMPLATE_ID.getKey(), templateId),
                entry(LETTER_TYPE.getKey(), LetterType.UPDATE_YOUR_ADDRESS.name()));
    }


    public static void verifyCardCreationEventFailExceptionAndEventAreCorrectWithCardId(Claim claim,
                                                                                        RuntimeException testException,
                                                                                        EventFailedException exception,
                                                                                        String cardAccountId) {
        verifyCardCreationEventFailExceptionAndEventAreCorrect(claim, testException, exception, "NewCardService.updateAndSaveClaim", cardAccountId);
    }

    public static void verifyCardCreationEventFailExceptionAndEventAreCorrectWithoutCardId(Claim claim,
                                                                                           RuntimeException testException,
                                                                                           EventFailedException exception) {
        verifyCardCreationEventFailExceptionAndEventAreCorrect(claim, testException, exception, "NewCardService.createNewCard", null);
    }

    public static void verifyCardCreationEventFailExceptionAndEventAreCorrect(Claim claim,
                                                                              RuntimeException testException,
                                                                              EventFailedException exception,
                                                                              String stackLocation,
                                                                              String cardAccountId) {
        String expectedFailureMessage = String.format("Card creation failed for claim %s, exception is: %s", claim.getId(), TEST_EXCEPTION_MESSAGE);
        verifyCommonEventFailureDetail(exception, testException, expectedFailureMessage, claim, ClaimEventType.NEW_CARD);
        Map<String, Object> metadata = exception.getFailureEvent().getEventMetadata();
        assertExceptionDetailCorrect(metadata, TEST_EXCEPTION_MESSAGE, stackLocation);
        assertThat(metadata).contains(entry(ClaimEventMetadataKey.CARD_ACCOUNT_ID.getKey(), cardAccountId));
    }

    private static void verifyCommonEventFailureDetail(EventFailedException exception, Exception failureCause, String expectedFailureMessage, Claim claim,
                                                       ClaimEventType failedEventType) {
        assertThat(exception).hasMessage(expectedFailureMessage);
        assertThat(exception).hasCause(failureCause);

        assertFailureMetadataCorrect(exception, expectedFailureMessage, claim, failedEventType);
    }

    private static void assertFailureMetadataCorrect(EventFailedException exception, String expectedFailureMessage, Claim claim,
                                                     ClaimEventType failedEventType) {
        FailureEvent failureEvent = exception.getFailureEvent();
        Map<String, Object> metadata = failureEvent.getEventMetadata();
        assertThat(failureEvent.getEventType()).isEqualTo(CommonEventType.FAILURE);
        assertThat(failureEvent.getTimestamp()).isNotNull();
        assertThat(metadata).contains(
                entry(FAILURE_DESCRIPTION_KEY, expectedFailureMessage),
                entry(CLAIM_ID.getKey(), claim.getId()),
                entry(FAILED_EVENT_KEY, failedEventType));
    }

    private static void assertExceptionDetailCorrect(Map<String, Object> metadata, String exceptionDetail, String methodStack) {
        String actualExceptionDetail = (String) metadata.get(EXCEPTION_DETAIL_KEY);
        assertThat(actualExceptionDetail).startsWith(exceptionDetail);
        assertThat(actualExceptionDetail).contains(methodStack);
    }
}
