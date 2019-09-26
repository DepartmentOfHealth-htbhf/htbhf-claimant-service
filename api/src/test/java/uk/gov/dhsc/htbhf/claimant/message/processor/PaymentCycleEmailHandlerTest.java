package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForChildUnderFourNotificationWhenChildTurnsFourInFirstWeekOfNextCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchersForUnderOne;

@ExtendWith(MockitoExtension.class)
class PaymentCycleEmailHandlerTest {

    private static final List<LocalDate> DATE_OF_BIRTH_OF_CHILDREN = List.of(
            LocalDate.now().minusMonths(6),
            LocalDate.now().minusYears(3).minusMonths(6));
    private static final Optional<LocalDate> NOT_PREGNANT = Optional.empty();
    private static final LocalDate START_OF_NEXT_CYCLE = LocalDate.now().plusDays(28);

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;
    @Mock
    private PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;
    @InjectMocks
    private PaymentCycleEmailHandler paymentCycleEmailHandler;

    @Test
    void shouldSendEmailForChildTurnsFourInFirstWeekOfNextCycle() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(nextPaymentCycleSummary);
        //This entitlement specifically has no vouchers for 1-4 year olds.
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementWithVouchersForUnderOne(START_OF_NEXT_CYCLE);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);

        paymentCycleEmailHandler.handleAdditionalEmails(paymentCycle);

        verify(childDateOfBirthCalculator).getNextPaymentCycleSummary(paymentCycle);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                DATE_OF_BIRTH_OF_CHILDREN,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verifyChildTurnsFourEmailNotificationSent(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendNoEmails() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(NextPaymentCycleSummary.NO_CHILDREN);

        paymentCycleEmailHandler.handleAdditionalEmails(paymentCycle);

        verify(childDateOfBirthCalculator).getNextPaymentCycleSummary(paymentCycle);
        verifyZeroInteractions(paymentCycleEntitlementCalculator, messageQueueClient);
    }

    private void verifyChildTurnsFourEmailNotificationSent(PaymentCycle paymentCycle, MessagePayload messagePayload) {
        EmailMessagePayload payload = (EmailMessagePayload) messagePayload;
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertEmailPayloadCorrectForChildUnderFourNotificationWhenChildTurnsFourInFirstWeekOfNextCycle(
                payload.getEmailPersonalisation(),
                paymentCycle.getCycleEndDate().plusDays(1));
    }

}
