package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.*;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory.buildEmailMessagePayloadWithFirstAndLastNameOnly;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.ADDITIONAL_PREGNANCY_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REPORT_CLAIM;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.INSTANT_SUCCESS;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.REPORT_A_BIRTH_REMINDER;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.LAST_NAME;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@ExtendWith(MockitoExtension.class)
class ClaimMessageSenderTest {

    private static final int CYCLE_DURATION_IN_DAYS = 28;
    private static final Duration REPORT_A_BIRTH_MESSAGE_DELAY = Duration.ZERO;

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private EmailMessagePayloadFactory emailMessagePayloadFactory;

    @InjectMocks
    private ClaimMessageSender claimMessageSender;

    @BeforeEach
    void setup() {
        claimMessageSender = new ClaimMessageSender(messageQueueClient, emailMessagePayloadFactory, CYCLE_DURATION_IN_DAYS, REPORT_A_BIRTH_MESSAGE_DELAY);
    }

    @Test
    void shouldSendReportClaimMessage() {
        Claim claim = aValidClaim();
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusYears(1));
        ClaimAction claimAction = ClaimAction.NEW;
        LocalDateTime now = LocalDateTime.now();

        claimMessageSender.sendReportClaimMessage(claim, datesOfBirthOfChildren, claimAction);

        ArgumentCaptor<ReportClaimMessagePayload> argumentCaptor = ArgumentCaptor.forClass(ReportClaimMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(REPORT_CLAIM));
        ReportClaimMessagePayload payload = argumentCaptor.getValue();
        assertThat(payload.getTimestamp()).isAfterOrEqualTo(now);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getDatesOfBirthOfChildren()).isEqualTo(datesOfBirthOfChildren);
        assertThat(payload.getClaimAction()).isEqualTo(claimAction);
    }

    @Test
    void shouldSendReportClaimMessageWithUpdatedClaimantFields() {
        Claim claim = aValidClaim();
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusYears(1));
        List<UpdatableClaimantField> updatedClaimantFields = List.of(LAST_NAME);
        LocalDateTime now = LocalDateTime.now();

        claimMessageSender.sendReportClaimMessageWithUpdatedClaimantFields(claim, datesOfBirthOfChildren, updatedClaimantFields);

        ArgumentCaptor<ReportClaimMessagePayload> argumentCaptor = ArgumentCaptor.forClass(ReportClaimMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(REPORT_CLAIM));
        ReportClaimMessagePayload payload = argumentCaptor.getValue();
        assertThat(payload.getTimestamp()).isAfterOrEqualTo(now);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getDatesOfBirthOfChildren()).isEqualTo(datesOfBirthOfChildren);
        assertThat(payload.getClaimAction()).isEqualTo(UPDATED);
        assertThat(payload.getUpdatedClaimantFields()).isEqualTo(updatedClaimantFields);
    }

    @Test
    void shouldSendNewCardMessage() {
        Claim claim = aValidClaim();
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);

        claimMessageSender.sendNewCardMessage(claim, decision);

        NewCardRequestMessagePayload newCardRequestMessagePayload = NewCardRequestMessagePayload.builder()
                .claimId(claim.getId())
                .eligibilityAndEntitlementDecision(decision)
                .build();
        verify(messageQueueClient).sendMessage(newCardRequestMessagePayload, CREATE_NEW_CARD);
    }

    @Test
    void shouldSendAdditionalPaymentMessage() {
        Claim claim = aValidClaim();

        claimMessageSender.sendAdditionalPaymentMessage(claim);

        AdditionalPregnancyPaymentMessagePayload expectedPayload = AdditionalPregnancyPaymentMessagePayload.builder()
                .claimId(claim.getId())
                .build();
        verify(messageQueueClient).sendMessage(expectedPayload, ADDITIONAL_PREGNANCY_PAYMENT);
    }

    @Test
    void shouldSendInstantSuccessMessage() {
        Claim claim = aValidClaim();
        EmailMessagePayload payload = mock(EmailMessagePayload.class);
        given(emailMessagePayloadFactory.buildEmailMessagePayload(any(), any(), any(), any())).willReturn(payload);
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus(ELIGIBLE);

        claimMessageSender.sendInstantSuccessEmailMessage(claim, decision);

        verify(messageQueueClient).sendMessage(payload, SEND_EMAIL);
        LocalDate expectedNextPaymentDate = claim.getClaimStatusTimestamp().toLocalDate().plusDays(CYCLE_DURATION_IN_DAYS);
        verify(emailMessagePayloadFactory).buildEmailMessagePayload(claim, decision.getVoucherEntitlement(), expectedNextPaymentDate, INSTANT_SUCCESS);
    }

    @Test
    void shouldSendReportABirthMessage() {
        Claim claim  = aValidClaim();

        claimMessageSender.sendReportABirthEmailMessage(claim);

        MessagePayload emailMessagePayload = buildEmailMessagePayloadWithFirstAndLastNameOnly(claim, REPORT_A_BIRTH_REMINDER);
        verify(messageQueueClient).sendMessageWithDelay(emailMessagePayload, SEND_EMAIL, REPORT_A_BIRTH_MESSAGE_DELAY);
    }

}
