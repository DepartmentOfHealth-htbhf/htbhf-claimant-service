package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessor;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.requestcontext.aop.NewRequestContextWithSessionId;

/**
 * Component responsible for scheduling the processing of messages.
 * Each MessageType has its own cron schedule - see https://stackoverflow.com/a/26147143 for examples.
 * Each messageType also has its own minimum and maximum lock period. For a definition of the ISO-8601 duration format, see
 * https://www.digi.com/resources/documentation/digidocs/90001437-13/reference/r_iso_8601_duration_format.htm.
 * Note that the {@link MessageProcessor} also specifies a limit on the number of messages to process
 * in one go.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MessageProcessorScheduler {

    private static final String EVERY_30_SECONDS = "*/30 * * * * *";
    private static final String ONE_HOUR = "PT60M";
    private static final String FIFTEEN_SECONDS = "PT15S";

    private final MessageProcessor messageProcessor;

    @Scheduled(cron = EVERY_30_SECONDS) 
    @SchedulerLock(
            name = "Process ADDITIONAL_PREGNANCY_PAYMENT messages",
            lockAtLeastForString = FIFTEEN_SECONDS,
            lockAtMostForString = ONE_HOUR)
    @NewRequestContextWithSessionId(sessionId = "MessageProcessor:ADDITIONAL_PREGNANCY_PAYMENT")
    public void processAdditionalPregnancyPaymentMessages() {
        messageProcessor.processMessagesOfType(MessageType.ADDITIONAL_PREGNANCY_PAYMENT);
    }

    @Scheduled(cron = "*/30 * 8-18 * * *")  // every 30 seconds between 8am and 6pm
    @SchedulerLock(
            name = "Process SEND_EMAIL messages",
            lockAtLeastForString = FIFTEEN_SECONDS,
            lockAtMostForString = ONE_HOUR)
    @NewRequestContextWithSessionId(sessionId = "MessageProcessor:SEND_EMAIL")
    public void processSendEmailMessages() {
        messageProcessor.processMessagesOfType(MessageType.SEND_EMAIL);
    }

    @Scheduled(cron = EVERY_30_SECONDS)
    @SchedulerLock(
            name = "Process CREATE_NEW_CARD messages",
            lockAtLeastForString = FIFTEEN_SECONDS,
            lockAtMostForString = ONE_HOUR)
    @NewRequestContextWithSessionId(sessionId = "MessageProcessor:CREATE_NEW_CARD")
    public void processCreateNewCardMessages() {
        messageProcessor.processMessagesOfType(MessageType.CREATE_NEW_CARD);
    }

    @Scheduled(cron = EVERY_30_SECONDS)
    @SchedulerLock(
            name = "Process DETERMINE_ENTITLEMENT messages",
            lockAtLeastForString = FIFTEEN_SECONDS,
            lockAtMostForString = ONE_HOUR)
    @NewRequestContextWithSessionId(sessionId = "MessageProcessor:DETERMINE_ENTITLEMENT")
    public void processDetermineEntitlementMessages() {
        messageProcessor.processMessagesOfType(MessageType.DETERMINE_ENTITLEMENT);
    }

    @Scheduled(cron = EVERY_30_SECONDS)
    @SchedulerLock(
            name = "Process MAKE_FIRST_PAYMENT messages",
            lockAtLeastForString = FIFTEEN_SECONDS,
            lockAtMostForString = ONE_HOUR)
    @NewRequestContextWithSessionId(sessionId = "MessageProcessor:MAKE_FIRST_PAYMENT")
    public void processFirstPaymentMessages() {
        messageProcessor.processMessagesOfType(MessageType.MAKE_FIRST_PAYMENT);
    }

    @Scheduled(cron = EVERY_30_SECONDS)
    @SchedulerLock(
            name = "Process MAKE_PAYMENT messages",
            lockAtLeastForString = FIFTEEN_SECONDS,
            lockAtMostForString = ONE_HOUR)
    @NewRequestContextWithSessionId(sessionId = "MessageProcessor:MAKE_PAYMENT")
    public void processPaymentMessages() {
        messageProcessor.processMessagesOfType(MessageType.MAKE_PAYMENT);
    }
}
