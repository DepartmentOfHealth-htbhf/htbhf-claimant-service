package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;

import java.time.Duration;
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
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildren;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HOMER_FORENAME;

@ExtendWith(MockitoExtension.class)
class UpcomingBirthdayEmailHandlerTest {

    private static final Optional<LocalDate> NOT_PREGNANT = Optional.empty();
    private static final LocalDate START_OF_NEXT_CYCLE = LocalDate.now().plusDays(28);
    private static final LocalDate UNDER_ONE_ALL_OF_NEXT_CYCLE = START_OF_NEXT_CYCLE.minusMonths(6);
    private static final LocalDate TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(4);
    private static final LocalDate TURNS_ONE_ON_DAY_OF_NEXT_PAYMENT_CYCLE = START_OF_NEXT_CYCLE.minusYears(1);
    private static final Integer NUMBER_OF_CALCULATION_PERIODS = 4;
    private static final Map<String, Object> COMMON_EMAIL_MAP = Map.of(FIRST_NAME.getTemplateKeyName(), HOMER_FORENAME);
    private static final Duration CHANGE_IN_PAYMENT_MESSAGE_DELAY = Duration.ofDays(3);

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;
    @Mock
    private EmailMessagePayloadFactory emailMessagePayloadFactory;

    private UpcomingBirthdayEmailHandler upcomingBirthdayEmailHandler;

    @BeforeEach
    void init() {
        upcomingBirthdayEmailHandler = new UpcomingBirthdayEmailHandler(
                NUMBER_OF_CALCULATION_PERIODS,
                CHANGE_IN_PAYMENT_MESSAGE_DELAY,
                messageQueueClient,
                paymentCycleEntitlementCalculator,
                emailMessagePayloadFactory);
    }

    @Test
    void shouldSendEmailForChildTurnsFourInNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        //This entitlement specifically has no vouchers for 1-4 year olds.
        List<LocalDate> childrensDob = List.of(UNDER_ONE_ALL_OF_NEXT_CYCLE, TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementMatchingChildren(START_OF_NEXT_CYCLE, childrensDob);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = buildPaymentCycle(childrensDob);
        given(emailMessagePayloadFactory.createCommonEmailPersonalisationMap(any(), any())).willReturn(new HashMap<>(COMMON_EMAIL_MAP));

        upcomingBirthdayEmailHandler.sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);

        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessageWithDelay(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL), eq(CHANGE_IN_PAYMENT_MESSAGE_DELAY));
        verify(emailMessagePayloadFactory).createCommonEmailPersonalisationMap(paymentCycle, nextEntitlement);
        verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInNextCycle(paymentCycle, payloadCaptor.getValue(), false);
    }

    @Test
    void shouldSendEmailForTwoChildrenTurningFourInNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(2).build();
        //This entitlement specifically has no vouchers for 1-4 year olds.
        List<LocalDate> childrensDob = List.of(
                UNDER_ONE_ALL_OF_NEXT_CYCLE, TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE, TURNS_FOUR_ON_DAY_ONE_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementMatchingChildren(START_OF_NEXT_CYCLE, childrensDob);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = buildPaymentCycle(childrensDob);
        given(emailMessagePayloadFactory.createCommonEmailPersonalisationMap(any(), any())).willReturn(new HashMap<>(COMMON_EMAIL_MAP));

        upcomingBirthdayEmailHandler.sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);

        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessageWithDelay(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL), eq(CHANGE_IN_PAYMENT_MESSAGE_DELAY));
        verify(emailMessagePayloadFactory).createCommonEmailPersonalisationMap(paymentCycle, nextEntitlement);
        verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInNextCycle(paymentCycle, payloadCaptor.getValue(), true);
    }

    @Test
    void shouldSendEmailForChildTurnsOneInNextCycle() {
        NextPaymentCycleSummary nextPaymentCycleSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).build();
        List<LocalDate> childrensDob = List.of(TURNS_ONE_ON_DAY_OF_NEXT_PAYMENT_CYCLE);
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementMatchingChildren(START_OF_NEXT_CYCLE, childrensDob);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        PaymentCycle paymentCycle = buildPaymentCycle(childrensDob);
        given(emailMessagePayloadFactory.createCommonEmailPersonalisationMap(any(), any())).willReturn(new HashMap<>(COMMON_EMAIL_MAP));

        upcomingBirthdayEmailHandler.sendChildTurnsOneEmail(paymentCycle, nextPaymentCycleSummary);

        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                NOT_PREGNANT,
                childrensDob,
                START_OF_NEXT_CYCLE,
                paymentCycle.getVoucherEntitlement()
        );
        ArgumentCaptor<EmailMessagePayload> payloadCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessageWithDelay(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL), eq(CHANGE_IN_PAYMENT_MESSAGE_DELAY));
        verify(emailMessagePayloadFactory).createCommonEmailPersonalisationMap(paymentCycle, nextEntitlement);
        verifyChildTurnsOneEmailNotificationSentWhenChildTurnsOneInNextCycle(paymentCycle, payloadCaptor.getValue());
    }

    private PaymentCycle buildPaymentCycle(List<LocalDate> childrensDob) {
        Claim claim = aClaimWithExpectedDeliveryDate(null);
        return aValidPaymentCycleBuilder().claim(claim).childrenDob(childrensDob).build();
    }

    private void verifyChildTurnsFourEmailNotificationSentWhenChildTurnsFourInNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload,
                                                                                        boolean multipleChildren) {
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        Map<String, Object> emailPersonalisation = payload.getEmailPersonalisation();
        assertThat(emailPersonalisation).contains(
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", ""),
                entry("multiple_children", multipleChildren)
        );
        assertThat(emailPersonalisation).containsAllEntriesOf(COMMON_EMAIL_MAP);
    }

    private void verifyChildTurnsOneEmailNotificationSentWhenChildTurnsOneInNextCycle(PaymentCycle paymentCycle, EmailMessagePayload payload) {
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
