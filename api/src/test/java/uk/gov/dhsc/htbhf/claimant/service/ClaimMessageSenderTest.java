package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.communications.TextMessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.*;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory.buildEmailMessagePayloadWithFirstAndLastNameOnly;
import static uk.gov.dhsc.htbhf.claimant.communications.LetterMessagePayloadFactory.buildLetterPayloadWithAddressAndPaymentFields;
import static uk.gov.dhsc.htbhf.claimant.communications.LetterMessagePayloadFactory.buildLetterPayloadWithAddressOnly;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.*;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.INSTANT_SUCCESS;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.INSTANT_SUCCESS_PARTIAL_CHILDREN_MATCH;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.PENDING_DECISION;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.REPORT_A_BIRTH_REMINDER;
import static uk.gov.dhsc.htbhf.claimant.message.payload.TextType.INSTANT_SUCCESS_TEXT;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.LAST_NAME;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

@ExtendWith(MockitoExtension.class)
class ClaimMessageSenderTest {

    private static final int CYCLE_DURATION_IN_DAYS = 28;
    private static final Duration REPORT_A_BIRTH_MESSAGE_DELAY = Duration.ZERO;

    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private EmailMessagePayloadFactory emailMessagePayloadFactory;
    @Mock
    private TextMessagePayloadFactory textMessagePayloadFactory;

    @InjectMocks
    private ClaimMessageSender claimMessageSender;

    @BeforeEach
    void setup() {
        claimMessageSender = new ClaimMessageSender(
                messageQueueClient,
                emailMessagePayloadFactory,
                textMessagePayloadFactory,
                CYCLE_DURATION_IN_DAYS,
                REPORT_A_BIRTH_MESSAGE_DELAY);
    }

    @Test
    void shouldSendReportClaimMessage() {
        Claim claim = aValidClaim();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        ClaimAction claimAction = ClaimAction.NEW;
        LocalDateTime now = LocalDateTime.now();

        claimMessageSender.sendReportClaimMessage(claim, identityAndEligibilityResponse, claimAction);

        ArgumentCaptor<ReportClaimMessagePayload> argumentCaptor = ArgumentCaptor.forClass(ReportClaimMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(REPORT_CLAIM));
        ReportClaimMessagePayload payload = argumentCaptor.getValue();
        assertThat(payload.getTimestamp()).isAfterOrEqualTo(now);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getIdentityAndEligibilityResponse()).isEqualTo(identityAndEligibilityResponse);
        assertThat(payload.getClaimAction()).isEqualTo(claimAction);
    }

    @Test
    void shouldSendReportClaimMessageWithUpdatedClaimantFields() {
        Claim claim = aValidClaim();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        List<UpdatableClaimantField> updatedClaimantFields = List.of(LAST_NAME);
        LocalDateTime now = LocalDateTime.now();

        claimMessageSender.sendReportClaimMessageWithUpdatedClaimantFields(claim, identityAndEligibilityResponse, updatedClaimantFields);

        ArgumentCaptor<ReportClaimMessagePayload> argumentCaptor = ArgumentCaptor.forClass(ReportClaimMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(REPORT_CLAIM));
        ReportClaimMessagePayload payload = argumentCaptor.getValue();
        assertThat(payload.getTimestamp()).isAfterOrEqualTo(now);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getIdentityAndEligibilityResponse()).isEqualTo(identityAndEligibilityResponse);
        assertThat(payload.getClaimAction()).isEqualTo(UPDATED);
        assertThat(payload.getUpdatedClaimantFields()).isEqualTo(updatedClaimantFields);
    }

    @Test
    void shouldSendNewCardMessage() {
        Claim claim = aValidClaim();
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);

        claimMessageSender.sendNewCardMessage(claim, decision);

        RequestNewCardMessagePayload expectedPayload = RequestNewCardMessagePayload.builder()
                .claimId(claim.getId())
                .eligibilityAndEntitlementDecision(decision)
                .build();
        verify(messageQueueClient).sendMessage(expectedPayload, REQUEST_NEW_CARD);
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

        claimMessageSender.sendInstantSuccessEmail(claim, decision, INSTANT_SUCCESS);

        verify(messageQueueClient).sendMessage(payload, SEND_EMAIL);
        LocalDate expectedNextPaymentDate = claim.getClaimStatusTimestamp().toLocalDate().plusDays(CYCLE_DURATION_IN_DAYS);
        verify(emailMessagePayloadFactory).buildEmailMessagePayload(claim, decision.getVoucherEntitlement(), expectedNextPaymentDate, INSTANT_SUCCESS);
    }

    @Test
    void shouldSendInstantSuccessTextMessage() {
        Claim claim = aValidClaim();
        TextMessagePayload payload = mock(TextMessagePayload.class);
        given(textMessagePayloadFactory.buildTextMessagePayload(any(), any(), any())).willReturn(payload);
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus(ELIGIBLE);

        claimMessageSender.sendInstantSuccessText(claim, decision, INSTANT_SUCCESS_TEXT);

        verify(messageQueueClient).sendMessage(payload, SEND_TEXT);
        verify(textMessagePayloadFactory).buildTextMessagePayload(claim, decision.getVoucherEntitlement(), INSTANT_SUCCESS_TEXT);
    }

    @Test
    void shouldSendInstantSuccessPartialChildrenMatchMessage() {
        Claim claim = aValidClaim();
        EmailMessagePayload payload = mock(EmailMessagePayload.class);
        given(emailMessagePayloadFactory.buildEmailMessagePayload(any(), any(), any(), any())).willReturn(payload);
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus(ELIGIBLE);

        claimMessageSender.sendInstantSuccessEmail(claim, decision, INSTANT_SUCCESS_PARTIAL_CHILDREN_MATCH);

        verify(messageQueueClient).sendMessage(payload, SEND_EMAIL);
        LocalDate expectedNextPaymentDate = claim.getClaimStatusTimestamp().toLocalDate().plusDays(CYCLE_DURATION_IN_DAYS);
        verify(emailMessagePayloadFactory).buildEmailMessagePayload(claim, decision.getVoucherEntitlement(), expectedNextPaymentDate,
                INSTANT_SUCCESS_PARTIAL_CHILDREN_MATCH);
    }

    @Test
    void shouldSendReportABirthMessage() {
        Claim claim  = aValidClaim();

        claimMessageSender.sendReportABirthEmailMessage(claim);

        MessagePayload expectedPayload = buildEmailMessagePayloadWithFirstAndLastNameOnly(claim, REPORT_A_BIRTH_REMINDER);
        verify(messageQueueClient).sendMessageWithDelay(expectedPayload, SEND_EMAIL, REPORT_A_BIRTH_MESSAGE_DELAY);
    }

    @Test
    void shouldSendDecisionPendingMessage() {
        Claim claim  = aValidClaim();

        claimMessageSender.sendDecisionPendingEmailMessage(claim);

        MessagePayload expectedPayload = buildEmailMessagePayloadWithFirstAndLastNameOnly(claim, PENDING_DECISION);
        verify(messageQueueClient).sendMessage(expectedPayload, SEND_EMAIL);
    }

    @Test
    void shouldSendLetterWithAddressOnlyMessage() {
        Claim claim = aValidClaim();

        claimMessageSender.sendLetterWithAddressOnlyMessage(claim, LetterType.UPDATE_YOUR_ADDRESS);

        LetterMessagePayload expectedPayload = buildLetterPayloadWithAddressOnly(claim, LetterType.UPDATE_YOUR_ADDRESS);
        verify(messageQueueClient).sendMessage(expectedPayload, SEND_LETTER);
    }

    @Test
    void shouldSendLetterWithAddressAndPaymentFieldsMessage() {
        Claim claim = aValidClaim();
        EligibilityAndEntitlementDecision decision = anEligibleDecision();

        claimMessageSender.sendLetterWithAddressAndPaymentFieldsMessage(claim, decision, LetterType.APPLICATION_SUCCESS_CHILDREN_MATCH);

        LetterMessagePayload expectedPayload = buildLetterPayloadWithAddressAndPaymentFields(claim, decision, LetterType.APPLICATION_SUCCESS_CHILDREN_MATCH);
        verify(messageQueueClient).sendMessage(expectedPayload, SEND_LETTER);
    }
}
