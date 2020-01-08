package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory;

import java.time.LocalDate;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;

@ExtendWith(MockitoExtension.class)
class PaymentCycleNotificationHandlerTest {

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private UpcomingBirthdayEmailHandler upcomingBirthdayEmailHandler;
    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;
    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    @Mock
    private NextPaymentCycleSummary nextPaymentCycleSummary;
    @Mock
    private EmailMessagePayloadFactory emailMessagePayloadFactory;

    @InjectMocks
    PaymentCycleNotificationHandler paymentCycleNotificationHandler;

    @Test
    public void shouldSendRegularPaymentEmailOnly() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle)).willReturn(nextPaymentCycleSummary);
        given(nextPaymentCycleSummary.hasChildrenTurningFour()).willReturn(false);
        given(nextPaymentCycleSummary.hasChildrenTurningOne()).willReturn(false);
        EmailMessagePayload emailMessagePayload = EmailMessagePayload.builder().build();
        given(emailMessagePayloadFactory.buildEmailMessagePayload(any(), any())).willReturn(emailMessagePayload);

        paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(paymentCycle);

        verifyPaymentEmailNotificationSent(paymentCycle, emailMessagePayload, REGULAR_PAYMENT);
        verifyNoInteractions(upcomingBirthdayEmailHandler);
    }

    @Test
    public void shouldSendRestartedPaymentEmailOnly() {
        // back dated vouchers would cause a NEW_CHILD_FROM_PREGNANCY email to be sent if the this were a regular payment
        // therefore adding the vouchers here to ensure that we're handling restarted payments differently than regular payments
        PaymentCycle paymentCycle = aPaymentCycleWithBackdatedVouchersOnly();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle)).willReturn(nextPaymentCycleSummary);
        given(nextPaymentCycleSummary.hasChildrenTurningFour()).willReturn(false);
        given(nextPaymentCycleSummary.hasChildrenTurningOne()).willReturn(false);
        EmailMessagePayload emailMessagePayload = EmailMessagePayload.builder().build();
        given(emailMessagePayloadFactory.buildEmailMessagePayload(any(), any())).willReturn(emailMessagePayload);

        paymentCycleNotificationHandler.sendNotificationEmailsForRestartedPayment(paymentCycle);

        verifyPaymentEmailNotificationSent(paymentCycle, emailMessagePayload, RESTARTED_PAYMENT);
        verifyNoInteractions(upcomingBirthdayEmailHandler);
    }

    @Test
    public void shouldSendNewChildFromPregnancyEmailOnly() {
        Claim claim = aClaimWithExpectedDeliveryDate(LocalDate.now().minusWeeks(8));
        PaymentCycleVoucherEntitlement voucherEntitlement =
                aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild(LocalDate.now(), asList(LocalDate.now().minusWeeks(6)));
        PaymentCycle paymentCycle = aPaymentCycleWithCycleEntitlementAndClaim(voucherEntitlement, claim);
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle)).willReturn(nextPaymentCycleSummary);
        given(nextPaymentCycleSummary.hasChildrenTurningFour()).willReturn(false);
        given(nextPaymentCycleSummary.hasChildrenTurningOne()).willReturn(false);
        EmailMessagePayload emailMessagePayload = EmailMessagePayload.builder().build();
        given(emailMessagePayloadFactory.buildEmailMessagePayload(any(), any())).willReturn(emailMessagePayload);

        paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(paymentCycle);

        verifyNewChildFromPregnancyEmailSent(paymentCycle, emailMessagePayload);
        verifyNoInteractions(upcomingBirthdayEmailHandler);
    }

    @Test
    public void shouldSendChildTurningOneEmail() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle)).willReturn(nextPaymentCycleSummary);
        given(nextPaymentCycleSummary.hasChildrenTurningFour()).willReturn(false);
        given(nextPaymentCycleSummary.hasChildrenTurningOne()).willReturn(true);

        paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(paymentCycle);

        verify(upcomingBirthdayEmailHandler).sendChildTurnsOneEmail(paymentCycle, nextPaymentCycleSummary);
        verifyNoMoreInteractions(upcomingBirthdayEmailHandler);
    }

    @Test
    public void shouldSendChildTurningFourEmail() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle)).willReturn(nextPaymentCycleSummary);
        given(nextPaymentCycleSummary.hasChildrenTurningFour()).willReturn(true);
        given(nextPaymentCycleSummary.hasChildrenTurningOne()).willReturn(false);
        given(nextPaymentCycleSummary.youngestChildTurnsFour()).willReturn(false);

        paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(paymentCycle);

        verify(upcomingBirthdayEmailHandler).sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
        verifyNoMoreInteractions(upcomingBirthdayEmailHandler);
    }

    @Test
    public void shouldSendChildTurningFourEmailWhenPregnantClaimantsYoungestChildTurnsFour() {
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(ClaimTestDataFactory.aClaimWithExpectedDeliveryDate(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS));
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle)).willReturn(nextPaymentCycleSummary);
        given(nextPaymentCycleSummary.hasChildrenTurningFour()).willReturn(true);
        given(nextPaymentCycleSummary.hasChildrenTurningOne()).willReturn(false);
        given(nextPaymentCycleSummary.youngestChildTurnsFour()).willReturn(true);
        given(pregnancyEntitlementCalculator.claimantIsPregnantAfterCycle(any())).willReturn(true);

        paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(paymentCycle);

        verify(upcomingBirthdayEmailHandler).sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
        verifyNoMoreInteractions(upcomingBirthdayEmailHandler);
    }

    @Test
    public void shouldSendYoungestChildTurningFourEmail() {
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(ClaimTestDataFactory.aClaimWithExpectedDeliveryDate(null));
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle)).willReturn(nextPaymentCycleSummary);
        given(nextPaymentCycleSummary.youngestChildTurnsFour()).willReturn(true);
        given(pregnancyEntitlementCalculator.claimantIsPregnantAfterCycle(any())).willReturn(false);

        paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(paymentCycle);

        verify(upcomingBirthdayEmailHandler).sendPaymentStoppingYoungestChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
        verifyNoMoreInteractions(upcomingBirthdayEmailHandler);
    }

    @Test
    public void shouldSendChildTurningOneAndFourEmails() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle)).willReturn(nextPaymentCycleSummary);
        given(nextPaymentCycleSummary.hasChildrenTurningFour()).willReturn(true);
        given(nextPaymentCycleSummary.hasChildrenTurningOne()).willReturn(true);
        given(nextPaymentCycleSummary.youngestChildTurnsFour()).willReturn(false);

        EmailMessagePayload emailMessagePayload = EmailMessagePayload.builder().build();
        given(emailMessagePayloadFactory.buildEmailMessagePayload(any(), any())).willReturn(emailMessagePayload);

        paymentCycleNotificationHandler.sendNotificationEmailsForRegularPayment(paymentCycle);

        verify(upcomingBirthdayEmailHandler).sendChildTurnsOneEmail(paymentCycle, nextPaymentCycleSummary);
        verify(upcomingBirthdayEmailHandler).sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
        verifyNoMoreInteractions(upcomingBirthdayEmailHandler);
    }

    private void verifyPaymentEmailNotificationSent(PaymentCycle paymentCycle, EmailMessagePayload emailMessagePayload, EmailType emailType) {
        verify(messageQueueClient).sendMessage(emailMessagePayload, MessageType.SEND_EMAIL);
        verify(emailMessagePayloadFactory).buildEmailMessagePayload(paymentCycle, emailType);
    }

    private void verifyNewChildFromPregnancyEmailSent(PaymentCycle paymentCycle, EmailMessagePayload emailMessagePayload) {
        verify(messageQueueClient).sendMessage(emailMessagePayload, MessageType.SEND_EMAIL);
        verify(emailMessagePayloadFactory).buildEmailMessagePayload(paymentCycle, NEW_CHILD_FROM_PREGNANCY);
    }
}
