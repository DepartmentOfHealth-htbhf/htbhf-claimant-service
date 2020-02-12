package uk.gov.dhsc.htbhf.claimant.communications;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;

@Component
@AllArgsConstructor
public class PaymentCycleNotificationHandler {

    private final MessageQueueClient messageQueueClient;
    private final ChildDateOfBirthCalculator childDateOfBirthCalculator;
    private final PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    private final UpcomingBirthdayEmailHandler upcomingBirthdayEmailHandler;
    private final EmailMessagePayloadFactory emailMessagePayloadFactory;

    /**
     * Sends a notification for a regular payment (claimant was previously active and is still active).
     * Sends an email of type {@link EmailType#REGULAR_PAYMENT}.
     *
     * @param paymentCycle the current payment cycle
     */
    public void sendNotificationEmailsForRegularPayment(PaymentCycle paymentCycle) {
        EmailType emailType = voucherEntitlementIndicatesNewChildFromPregnancy(paymentCycle)
                ? EmailType.NEW_CHILD_FROM_PREGNANCY
                : EmailType.REGULAR_PAYMENT;
        sendNotificationEmail(paymentCycle, emailType);
    }

    /**
     * Sends a notification for a restarted payment (claimant was pending_expiry and is now active).
     * Sends an email of type {@link EmailType#RESTARTED_PAYMENT}.
     *
     * @param paymentCycle the current payment cycle
     */
    public void sendNotificationEmailsForRestartedPayment(PaymentCycle paymentCycle) {
        sendNotificationEmail(paymentCycle, EmailType.RESTARTED_PAYMENT);
    }

    private void sendNotificationEmail(PaymentCycle paymentCycle, EmailType emailType) {
        if (StringUtils.isNotEmpty(paymentCycle.getClaim().getClaimant().getEmailAddress())) {
            EmailMessagePayload messagePayload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, emailType);
            messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
            handleUpcomingBirthdayEmails(paymentCycle);
        }
    }

    private boolean voucherEntitlementIndicatesNewChildFromPregnancy(PaymentCycle paymentCycle) {
        return paymentCycle.getVoucherEntitlement().getBackdatedVouchers() > 0;
    }

    private void handleUpcomingBirthdayEmails(PaymentCycle paymentCycle) {
        NextPaymentCycleSummary nextPaymentCycleSummary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        if (nextPaymentCycleSummary.youngestChildTurnsFour() && !pregnancyEntitlementCalculator.claimantIsPregnantAfterCycle(paymentCycle)) {
            upcomingBirthdayEmailHandler.sendPaymentStoppingYoungestChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
        } else {
            if (nextPaymentCycleSummary.hasChildrenTurningFour()) {
                upcomingBirthdayEmailHandler.sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
            }
            if (nextPaymentCycleSummary.hasChildrenTurningOne()) {
                upcomingBirthdayEmailHandler.sendChildTurnsOneEmail(paymentCycle, nextPaymentCycleSummary);
            }
        }
    }
}