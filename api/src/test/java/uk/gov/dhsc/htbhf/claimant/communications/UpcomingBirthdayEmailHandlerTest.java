package uk.gov.dhsc.htbhf.claimant.communications;

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
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_FIRST_NAME;

@ExtendWith(MockitoExtension.class)
class UpcomingBirthdayEmailHandlerTest {

    private static final Optional<LocalDate> NOT_PREGNANT = Optional.empty();
    private static final LocalDate START_OF_NEXT_CYCLE = LocalDate.now().plusDays(28);
    private static final LocalDate UNDER_ONE_ALL_OF_NEXT_CYCLE = START_OF_NEXT_CYCLE.minusMonths(6);
    private static final LocalDate TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(4);
    private static final LocalDate TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(4).plusDays(4);
    private static final LocalDate TURNS_ONE_ON_DAY_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(1);
    private static final LocalDate TURNS_ONE_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(1).plusDays(4);
    private static final Integer NUMBER_OF_CALCULATION_PERIODS = 4;
    private static final Map<String, Object> COMMON_EMAIL_MAP = Map.of(FIRST_NAME.getTemplateKeyName(), VALID_FIRST_NAME);

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;
    @Mock
    private EmailMessagePayloadFactory emailMessagePayloadFactory;

    private UpcomingBirthdayEmailHandler upcomingBirthdayEmailHandler;

    @BeforeEach
    public void init() {
        upcomingBirthdayEmailHandler = new UpcomingBirthdayEmailHandler(
                NUMBER_OF_CALCULATION_PERIODS,
                messageQueueClient,
                paymentCycleEntitlementCalculator,
                emailMessagePayloadFactory);
    }

    @Test
    void shouldSendEmailForChildTurnsFourOnFirstDayOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        //This entitlement specifically has no vouchers for 1-4 year olds.
        List<LocalDate> childrensDob = List.of(UNDER_ONE_ALL_OF_NEXT_CYCLE, TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementMatchingChildren(START_OF_NEXT_CYCLE, childrensDob);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder().childrenDob(childrensDob).build();
        given(emailMessagePayloadFactory.createCommonEmailPersonalisationMap(any(), any())).willReturn(new HashMap<>(COMMON_EMAIL_MAP));

        upcomingBirthdayEmailHandler.sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);

        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verify(emailMessagePayloadFactory).createCommonEmailPersonalisationMap(paymentCycle, nextEntitlement);
        verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourOnFirstDayOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendEmailForChildTurnsFourInFirstWeekOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        //This entitlement specifically has one voucher for 1-4 year olds.
        List<LocalDate> childrensDob = List.of(UNDER_ONE_ALL_OF_NEXT_CYCLE, TURNS_FOUR_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementMatchingChildren(START_OF_NEXT_CYCLE, childrensDob);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder().childrenDob(childrensDob).build();
        given(emailMessagePayloadFactory.createCommonEmailPersonalisationMap(any(), any())).willReturn(new HashMap<>(COMMON_EMAIL_MAP));

        upcomingBirthdayEmailHandler.sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);

        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verify(emailMessagePayloadFactory).createCommonEmailPersonalisationMap(paymentCycle, nextEntitlement);
        verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInFirstWeekOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendEmailForChildTurnsOneOnFirstDayOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).build();
        List<LocalDate> childrensDob = List.of(TURNS_ONE_ON_DAY_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementMatchingChildren(START_OF_NEXT_CYCLE, childrensDob);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder().childrenDob(childrensDob).build();
        given(emailMessagePayloadFactory.createCommonEmailPersonalisationMap(any(), any())).willReturn(new HashMap<>(COMMON_EMAIL_MAP));

        upcomingBirthdayEmailHandler.sendChildTurnsOneEmail(paymentCycle, nextPaymentCycleSummary);

        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verify(emailMessagePayloadFactory).createCommonEmailPersonalisationMap(paymentCycle, nextEntitlement);
        verifyChildTurnsOneEmailNotificationSentWhenChildTurnsFourOneFirstDayOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    @Test
    void shouldSendEmailForChildTurnsOneInFirstWeekOfNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).build();
        List<LocalDate> childrensDob = List.of(TURNS_ONE_IN_FIRST_WEEK_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementMatchingChildren(START_OF_NEXT_CYCLE, childrensDob);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder().childrenDob(childrensDob).build();
        given(emailMessagePayloadFactory.createCommonEmailPersonalisationMap(any(), any())).willReturn(new HashMap<>(COMMON_EMAIL_MAP));

        upcomingBirthdayEmailHandler.sendChildTurnsOneEmail(paymentCycle, nextPaymentCycleSummary);

        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));
        verify(emailMessagePayloadFactory).createCommonEmailPersonalisationMap(paymentCycle, nextEntitlement);
        verifyChildTurnsOneEmailNotificationSentWhenChildTurnsOneInFirstWeekOfNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    private void verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourOnFirstDayOfNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        Map<String, Object> emailPersonalisation = payload.getEmailPersonalisation();
        assertThat(emailPersonalisation).contains(
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", ""),
                entry("multiple_children", false)
        );
        assertThat(emailPersonalisation).containsAllEntriesOf(COMMON_EMAIL_MAP);
    }

    private void verifyChildTurnsOneEmailNotificationSentWhenChildTurnsFourOneFirstDayOfNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_ONE);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        Map<String, Object> emailPersonalisation = payload.getEmailPersonalisation();
        assertThat(emailPersonalisation).contains(
                entry("children_under_1_payment", ""),
                entry("children_under_4_payment", "\n* £12.40 for children between 1 and 4"),
                entry("multiple_children", false)
        );
        assertThat(emailPersonalisation).containsAllEntriesOf(COMMON_EMAIL_MAP);
    }

    private void verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInFirstWeekOfNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        Map<String, Object> emailPersonalisation = payload.getEmailPersonalisation();
        assertThat(emailPersonalisation).contains(
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", ""),
                entry("multiple_children", false)
        );
        assertThat(emailPersonalisation).containsAllEntriesOf(COMMON_EMAIL_MAP);
    }

    private void verifyChildTurnsOneEmailNotificationSentWhenChildTurnsOneInFirstWeekOfNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_ONE);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        Map<String, Object> emailPersonalisation = payload.getEmailPersonalisation();
        assertThat(emailPersonalisation).contains(
                entry("children_under_1_payment", ""),
                entry("children_under_4_payment", "\n* £12.40 for children between 1 and 4"),
                entry("multiple_children", false)
        );
        assertThat(emailPersonalisation).containsAllEntriesOf(COMMON_EMAIL_MAP);
    }

}
