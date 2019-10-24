package uk.gov.dhsc.htbhf.claimant.reporting;

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
import uk.gov.dhsc.htbhf.claimant.message.payload.ReportPaymentMessagePayload;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REPORT_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.INITIAL_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.SCHEDULED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.TOP_UP_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaimAndChildrenDobs;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR;

@ExtendWith(MockitoExtension.class)
class ReportPaymentMessageSenderTest {

    @Mock
    private MessageQueueClient messageQueueClient;

    @InjectMocks
    private ReportPaymentMessageSender reportPaymentMessageSender;

    @Test
    void shouldReportTopUpPayment() {
        Claim claim = aValidClaim();
        List<LocalDate> datesOfBirthOfChildren = ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR;
        PaymentCycle paymentCycle = aPaymentCycleWithClaimAndChildrenDobs(claim, datesOfBirthOfChildren);
        int paymentForPregnancy = 100;
        LocalDateTime testStart = LocalDateTime.now();

        reportPaymentMessageSender.sendReportTopUpPaymentMessage(claim, paymentCycle, paymentForPregnancy);

        ArgumentCaptor<ReportPaymentMessagePayload> argumentCaptor = ArgumentCaptor.forClass(ReportPaymentMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(REPORT_PAYMENT));
        ReportPaymentMessagePayload payload = argumentCaptor.getValue();
        assertThat(payload.getTimestamp()).isAfterOrEqualTo(testStart);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(payload.getDatesOfBirthOfChildren()).isEqualTo(datesOfBirthOfChildren);
        assertThat(payload.getPaymentAction()).isEqualTo(TOP_UP_PAYMENT);
        assertThat(payload.getPaymentForChildrenUnderOne()).isZero();
        assertThat(payload.getPaymentForChildrenBetweenOneAndFour()).isZero();
        assertThat(payload.getPaymentForPregnancy()).isEqualTo(paymentForPregnancy);
    }

    @Test
    void shouldReportInitialPayment() {
        Claim claim = aValidClaim();
        List<LocalDate> datesOfBirthOfChildren = ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR;
        PaymentCycle paymentCycle = aPaymentCycleWithClaimAndChildrenDobs(claim, datesOfBirthOfChildren);
        LocalDateTime testStart = LocalDateTime.now();

        reportPaymentMessageSender.sendReportInitialPaymentMessage(claim, paymentCycle);

        verifyNonTopUpPaymentMessageSent(claim, datesOfBirthOfChildren, paymentCycle, testStart, INITIAL_PAYMENT);
    }

    @Test
    void shouldReportScheduledPayment() {
        Claim claim = aValidClaim();
        List<LocalDate> datesOfBirthOfChildren = ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR;
        PaymentCycle paymentCycle = aPaymentCycleWithClaimAndChildrenDobs(claim, datesOfBirthOfChildren);
        LocalDateTime testStart = LocalDateTime.now();

        reportPaymentMessageSender.sendReportScheduledPayment(claim, paymentCycle);

        verifyNonTopUpPaymentMessageSent(claim, datesOfBirthOfChildren, paymentCycle, testStart, SCHEDULED_PAYMENT);
    }

    private void verifyNonTopUpPaymentMessageSent(Claim claim,
                                                  List<LocalDate> datesOfBirthOfChildren,
                                                  PaymentCycle paymentCycle,
                                                  LocalDateTime testStart,
                                                  PaymentAction paymentAction) {
        ArgumentCaptor<ReportPaymentMessagePayload> argumentCaptor = ArgumentCaptor.forClass(ReportPaymentMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(REPORT_PAYMENT));
        ReportPaymentMessagePayload payload = argumentCaptor.getValue();
        assertThat(payload.getTimestamp()).isAfterOrEqualTo(testStart);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(payload.getPaymentAction()).isEqualTo(paymentAction);
        assertThat(payload.getDatesOfBirthOfChildren()).isEqualTo(datesOfBirthOfChildren);
        PaymentCycleVoucherEntitlement voucherEntitlement = paymentCycle.getVoucherEntitlement();
        int voucherValue = voucherEntitlement.getSingleVoucherValueInPence();
        assertThat(payload.getPaymentForChildrenUnderOne()).isEqualTo(voucherEntitlement.getVouchersForChildrenUnderOne() * voucherValue);
        assertThat(payload.getPaymentForChildrenBetweenOneAndFour()).isEqualTo(voucherEntitlement.getVouchersForChildrenBetweenOneAndFour() * voucherValue);
        assertThat(payload.getPaymentForPregnancy()).isEqualTo(voucherEntitlement.getVouchersForPregnancy() * voucherValue);
    }
}
