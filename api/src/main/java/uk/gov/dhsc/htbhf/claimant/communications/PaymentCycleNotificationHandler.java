package uk.gov.dhsc.htbhf.claimant.communications;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
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
    private final UpcomingBirthdayEmailHandler upcomingBirthdayEmailHandler;
    private final EmailMessagePayloadFactory emailMessagePayloadFactory;

    public void sendNotificationEmails(PaymentCycle paymentCycle) {
        EmailMessagePayload messagePayload = voucherEntitlementIndicatesNewChildFromPregnancy(paymentCycle)
                ? emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.NEW_CHILD_FROM_PREGNANCY)
                : emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.PAYMENT);
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
        handleUpcomingBirthdayEmails(paymentCycle);
    }

    private boolean voucherEntitlementIndicatesNewChildFromPregnancy(PaymentCycle paymentCycle) {
        return paymentCycle.getVoucherEntitlement().getBackdatedVouchers() > 0;
    }

    private void handleUpcomingBirthdayEmails(PaymentCycle paymentCycle) {
        NextPaymentCycleSummary nextPaymentCycleSummary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        if (nextPaymentCycleSummary.hasChildrenTurningFour()) {
            upcomingBirthdayEmailHandler.sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
        }
        if (nextPaymentCycleSummary.hasChildrenTurningOne()) {
            upcomingBirthdayEmailHandler.sendChildTurnsOneEmail(paymentCycle, nextPaymentCycleSummary);
        }
    }
}
