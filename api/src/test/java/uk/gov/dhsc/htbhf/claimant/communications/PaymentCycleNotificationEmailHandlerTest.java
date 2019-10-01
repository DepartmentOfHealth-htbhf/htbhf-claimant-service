package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithAllVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class PaymentCycleNotificationEmailHandlerTest {

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private UpcomingBirthdayEmailHandler upcomingBirthdayEmailHandler;

    @InjectMocks
    PaymentCycleNotificationEmailHandler paymentCycleNotificationEmailHandler;

    @Test
    public void shouldSendEmailAndInvokeUpcomingBirthdayEmailHandler() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();

        paymentCycleNotificationEmailHandler.sendNotificationEmails(paymentCycle);

        verifyPaymentEmailNotificationSent(paymentCycle, claim);
        verify(upcomingBirthdayEmailHandler).handleUpcomingBirthdayEmails(paymentCycle);
    }

    private void verifyPaymentEmailNotificationSent(PaymentCycle paymentCycle, Claim claim) {
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));

        assertThat(payloadCaptor.getAllValues()).hasSize(1);
        assertThat(payloadCaptor.getValue()).isInstanceOf(EmailMessagePayload.class);
        verifyPaymentEmail(paymentCycle, claim, payloadCaptor.getValue());
    }

    private void verifyPaymentEmail(PaymentCycle paymentCycle, Claim claim, MessagePayload messagePayload) {
        EmailMessagePayload payload = (EmailMessagePayload) messagePayload;
        assertThat(payload.getEmailType()).isEqualTo(EmailType.PAYMENT);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), paymentCycle.getCycleEndDate().plusDays(1));
    }

}