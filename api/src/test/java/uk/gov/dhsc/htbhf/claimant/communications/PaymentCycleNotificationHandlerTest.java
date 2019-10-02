package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;

import java.time.LocalDate;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithAllVouchers;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertThatEmailPayloadCorrectForBackdatedPayment;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleEntitlementAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild;

@ExtendWith(MockitoExtension.class)
class PaymentCycleNotificationHandlerTest {

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private UpcomingBirthdayEmailHandler upcomingBirthdayEmailHandler;

    @InjectMocks
    PaymentCycleNotificationHandler paymentCycleNotificationHandler;

    @Test
    public void shouldSendEmailAndInvokeUpcomingBirthdayEmailHandler() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();

        paymentCycleNotificationHandler.sendNotificationEmails(paymentCycle);

        verifyPaymentEmailNotificationSent(paymentCycle, claim);
        verify(upcomingBirthdayEmailHandler).handleUpcomingBirthdayEmails(paymentCycle);
    }

    @Disabled("HTBHF-2028")
    @Test
    public void shouldSendNewChildFromPregnancyEmail() {
        Claim claim = aClaimWithExpectedDeliveryDate(LocalDate.now().minusWeeks(8));
        PaymentCycleVoucherEntitlement voucherEntitlement =
                aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild(LocalDate.now(), asList(LocalDate.now().minusWeeks(6)));
        PaymentCycle paymentCycle = aPaymentCycleWithCycleEntitlementAndClaim(voucherEntitlement, claim);

        paymentCycleNotificationHandler.sendNotificationEmails(paymentCycle);

        verifyNewChildFromPregnancyEmailSent(paymentCycle, claim);
        verify(upcomingBirthdayEmailHandler).handleUpcomingBirthdayEmails(paymentCycle);
    }

    private void verifyPaymentEmailNotificationSent(PaymentCycle paymentCycle, Claim claim) {
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));

        assertThat(payloadCaptor.getAllValues()).hasSize(1);
        assertThat(payloadCaptor.getValue()).isInstanceOf(EmailMessagePayload.class);
        verifyPaymentEmail(paymentCycle, claim, payloadCaptor.getValue());
    }

    private void verifyNewChildFromPregnancyEmailSent(PaymentCycle paymentCycle, Claim claim) {
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));

        assertThat(payloadCaptor.getAllValues()).hasSize(1);
        assertThat(payloadCaptor.getValue()).isInstanceOf(EmailMessagePayload.class);

        EmailMessagePayload payload = (EmailMessagePayload) payloadCaptor.getValue();
        assertThat(payload.getEmailType()).isEqualTo(EmailType.NEW_CHILD_FROM_PREGNANCY);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());

        assertThatEmailPayloadCorrectForBackdatedPayment(payload.getEmailPersonalisation(), paymentCycle);

    }

    private void verifyPaymentEmail(PaymentCycle paymentCycle, Claim claim, MessagePayload messagePayload) {
        EmailMessagePayload payload = (EmailMessagePayload) messagePayload;
        assertThat(payload.getEmailType()).isEqualTo(EmailType.PAYMENT);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), paymentCycle.getCycleEndDate().plusDays(1));
    }

}