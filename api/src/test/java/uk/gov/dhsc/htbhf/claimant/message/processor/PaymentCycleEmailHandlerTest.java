package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_LAST_NAME;

@ExtendWith(MockitoExtension.class)
class PaymentCycleEmailHandlerTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Optional<LocalDate> NOT_PREGNANT = Optional.empty();
    private static final LocalDate START_OF_NEXT_CYCLE = LocalDate.now().plusDays(28);
    private static final LocalDate UNDER_ONE_ALL_OF_NEXT_CYCLE = START_OF_NEXT_CYCLE.minusMonths(6);
    private static final LocalDate TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(4);
    private static final LocalDate TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(4).plusDays(4);
    private static final Integer NUMBER_OF_CALCULATION_PERIODS = 4;

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;
    @Mock
    private PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;

    private PaymentCycleEmailHandler paymentCycleEmailHandler;

    @BeforeEach
    public void init() {
        paymentCycleEmailHandler = new PaymentCycleEmailHandler(
                NUMBER_OF_CALCULATION_PERIODS,
                messageQueueClient,
                childDateOfBirthCalculator,
                paymentCycleEntitlementCalculator);
    }

    @Test
    void shouldSendEmailForChildTurnsFourInFirstWeekOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(nextPaymentCycleSummary);
        //This entitlement specifically has no vouchers for 1-4 year olds.
        List<LocalDate> childrensDob = List.of(UNDER_ONE_ALL_OF_NEXT_CYCLE, TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlement(START_OF_NEXT_CYCLE, childrensDob, LocalDate.now().plusMonths(2));
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder().childrenDob(childrensDob).build();

        paymentCycleEmailHandler.handleAdditionalEmails(paymentCycle);

        verify(childDateOfBirthCalculator).getNextPaymentCycleSummary(paymentCycle);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInFirstWeekOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendEmailForChildTurnsFourInSecondWeekOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(nextPaymentCycleSummary);
        //This entitlement specifically has one voucher for 1-4 year olds.
        List<LocalDate> childrensDob = List.of(UNDER_ONE_ALL_OF_NEXT_CYCLE, TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlement(START_OF_NEXT_CYCLE, childrensDob, LocalDate.now().plusMonths(2));
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder().childrenDob(childrensDob).build();

        paymentCycleEmailHandler.handleAdditionalEmails(paymentCycle);

        verify(childDateOfBirthCalculator).getNextPaymentCycleSummary(paymentCycle);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInSecondWeekOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendNoEmails() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(NextPaymentCycleSummary.NO_CHILDREN);

        paymentCycleEmailHandler.handleAdditionalEmails(paymentCycle);

        verify(childDateOfBirthCalculator).getNextPaymentCycleSummary(paymentCycle);
        verifyZeroInteractions(paymentCycleEntitlementCalculator, messageQueueClient);
    }

    private void verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInFirstWeekOfNextCycle(PaymentCycle paymentCycle, MessagePayload messagePayload) {
        EmailMessagePayload payload = (EmailMessagePayload) messagePayload;
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertEmailPayloadWhenChildTurnsFourOnFirstDayOfNextCycle(
                payload.getEmailPersonalisation(),
                paymentCycle.getCycleEndDate().plusDays(1));
    }

    private void verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInSecondWeekOfNextCycle(PaymentCycle paymentCycle, MessagePayload messagePayload) {
        EmailMessagePayload payload = (EmailMessagePayload) messagePayload;
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertEmailPayloadWhenChildTurnsFourInFirstWeekOfNextCycle(
                payload.getEmailPersonalisation(),
                paymentCycle.getCycleEndDate().plusDays(1));
    }

    private void assertEmailPayloadWhenChildTurnsFourOnFirstDayOfNextCycle(Map<String, Object> emailPersonalisation,
                                                                           LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£37.20"),
                entry("pregnancy_payment", "\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", ""),
                entry("multiple_children", false),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate)),
                entry("regular_payment", "£37.20")
        );
    }

    private void assertEmailPayloadWhenChildTurnsFourInFirstWeekOfNextCycle(Map<String, Object> emailPersonalisation,
                                                                            LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£40.30"),
                entry("pregnancy_payment", "\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", ""),
                entry("multiple_children", false),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate)),
                entry("regular_payment", "£37.20")
        );
    }

}
