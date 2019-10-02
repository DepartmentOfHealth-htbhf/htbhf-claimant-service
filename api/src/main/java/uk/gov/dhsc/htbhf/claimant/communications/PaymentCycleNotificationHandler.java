package uk.gov.dhsc.htbhf.claimant.communications;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildEmailMessagePayload;

@Component
@AllArgsConstructor
public class PaymentCycleNotificationHandler {

    private MessageQueueClient messageQueueClient;
    private UpcomingBirthdayEmailHandler upcomingBirthdayEmailHandler;

    public void sendNotificationEmails(PaymentCycle paymentCycle) {
        EmailMessagePayload messagePayload = voucherEntitlementIndicatesNewChildFromPregnancy(paymentCycle)
                ? buildEmailMessagePayload(paymentCycle, EmailType.NEW_CHILD_FROM_PREGNANCY)
                : buildEmailMessagePayload(paymentCycle, EmailType.PAYMENT);
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
        upcomingBirthdayEmailHandler.handleUpcomingBirthdayEmails(paymentCycle);
    }

    private boolean voucherEntitlementIndicatesNewChildFromPregnancy(PaymentCycle paymentCycle) {
        return paymentCycle.getVoucherEntitlement().getBackdatedVouchers() > 0;
    }
}
