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
    private static final LocalDate TURNS_ONE_ON_DAY_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(1);
    private static final LocalDate TURNS_ONE_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(1).plusDays(4);
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
    void shouldSendEmailForChildTurnsFourOnFirstDayOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(nextPaymentCycleSummary);
        //This entitlement specifically has no vouchers for 1-4 year olds.
        List<LocalDate> childrensDob = List.of(UNDER_ONE_ALL_OF_NEXT_CYCLE, TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlement(START_OF_NEXT_CYCLE, childrensDob);
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
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourOnFirstDayOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendEmailForChildTurnsFourInFirstWeekOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(nextPaymentCycleSummary);
        //This entitlement specifically has one voucher for 1-4 year olds.
        List<LocalDate> childrensDob = List.of(UNDER_ONE_ALL_OF_NEXT_CYCLE, TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlement(START_OF_NEXT_CYCLE, childrensDob);
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
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInFirstWeekOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendEmailForChildTurnsOneOnFirstDayOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).build();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(nextPaymentCycleSummary);
        List<LocalDate> childrensDob = List.of(TURNS_ONE_ON_DAY_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlement(START_OF_NEXT_CYCLE, childrensDob);
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
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verifyChildTurnsOneEmailNotificationSentWhenChildTurnsFourOneFirstDayfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendEmailForChildTurnsOneInFirstWeekOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).build();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(nextPaymentCycleSummary);
        List<LocalDate> childrensDob = List.of(TURNS_ONE_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlement(START_OF_NEXT_CYCLE, childrensDob);
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
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verifyChildTurnsOneEmailNotificationSentWhenChildTurnsOneInFirstWeekOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendNoEmails() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(any())).willReturn(NextPaymentCycleSummary.NO_CHILDREN);

        paymentCycleEmailHandler.handleAdditionalEmails(paymentCycle);

        verify(childDateOfBirthCalculator).getNextPaymentCycleSummary(paymentCycle);
        verifyZeroInteractions(paymentCycleEntitlementCalculator, messageQueueClient);
    }

    private void verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourOnFirstDayOfNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertEmailPayloadWhenChildTurnsFourOnFirstDayOfNextCycle(
                payload.getEmailPersonalisation(),
                paymentCycle.getCycleEndDate().plusDays(1));
    }

    private void verifyChildTurnsOneEmailNotificationSentWhenChildTurnsFourOneFirstDayfNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_ONE);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertEmailPayloadWhenChildTurnsOneOnFirstDayOfNextCycle(
                payload.getEmailPersonalisation(),
                paymentCycle.getCycleEndDate().plusDays(1));
    }

    private void verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInFirstWeekOfNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertEmailPayloadWhenChildTurnsFourInFirstWeekOfNextCycle(
                payload.getEmailPersonalisation(),
                paymentCycle.getCycleEndDate().plusDays(1));
    }

    private void verifyChildTurnsOneEmailNotificationSentWhenChildTurnsOneInFirstWeekOfNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_ONE);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertEmailPayloadWhenChildTurnsOneInFirstWeekOfNextCycle(
                payload.getEmailPersonalisation(),
                paymentCycle.getCycleEndDate().plusDays(1));
    }

    private void assertEmailPayloadWhenChildTurnsFourOnFirstDayOfNextCycle(Map<String, Object> emailPersonalisation,
                                                                           LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£24.80"),
                entry("pregnancy_payment", ""),
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", ""),
                entry("multiple_children", false),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate)),
                entry("regular_payment", "£24.80")
        );
    }

    private void assertEmailPayloadWhenChildTurnsOneOnFirstDayOfNextCycle(Map<String, Object> emailPersonalisation,
                                                                          LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£12.40"),
                entry("pregnancy_payment", ""),
                entry("children_under_1_payment", ""),
                entry("children_under_4_payment", "\n* £12.40 for children between 1 and 4"),
                entry("multiple_children", false),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate)),
                entry("regular_payment", "£12.40")
        );
    }

    private void assertEmailPayloadWhenChildTurnsFourInFirstWeekOfNextCycle(Map<String, Object> emailPersonalisation,
                                                                            LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£27.90"),
                entry("pregnancy_payment", ""),
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", ""),
                entry("multiple_children", false),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate)),
                entry("regular_payment", "£24.80")
        );
    }

    private void assertEmailPayloadWhenChildTurnsOneInFirstWeekOfNextCycle(Map<String, Object> emailPersonalisation,
                                                                            LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£15.50"),
                entry("pregnancy_payment", ""),
                entry("children_under_1_payment", ""),
                entry("children_under_4_payment", "\n* £12.40 for children between 1 and 4"),
                entry("multiple_children", false),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate)),
                entry("regular_payment", "£12.40")
        );
    }

}
