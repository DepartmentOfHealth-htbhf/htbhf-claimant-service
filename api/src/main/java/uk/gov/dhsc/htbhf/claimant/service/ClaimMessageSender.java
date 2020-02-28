package uk.gov.dhsc.htbhf.claimant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.communications.TextMessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.*;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory.buildEmailMessagePayloadWithFirstAndLastNameOnly;
import static uk.gov.dhsc.htbhf.claimant.communications.LetterMessagePayloadFactory.buildLetterPayloadWithAddressAndPaymentFields;
import static uk.gov.dhsc.htbhf.claimant.communications.LetterMessagePayloadFactory.buildLetterPayloadWithAddressOnly;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildReportClaimMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.*;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.PENDING_DECISION;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.REPORT_A_BIRTH_REMINDER;

/**
 * Responsible for sending messages related to claims (new or updated).
 */
@Component
public class ClaimMessageSender {

    private final MessageQueueClient messageQueueClient;
    private final EmailMessagePayloadFactory emailMessagePayloadFactory;
    private final TextMessagePayloadFactory textMessagePayloadFactory;
    private final Integer cycleDurationInDays;
    private final Duration reportABirthMessageDelay;

    public ClaimMessageSender(MessageQueueClient messageQueueClient,
                              EmailMessagePayloadFactory emailMessagePayloadFactory,
                              TextMessagePayloadFactory textMessagePayloadFactory,
                              @Value("${payment-cycle.cycle-duration-in-days}") Integer cycleDurationInDays,
                              @Value("${payment-cycle.report-a-birth-message-delay}") Duration reportABirthMessageDelay) {
        this.messageQueueClient = messageQueueClient;
        this.emailMessagePayloadFactory = emailMessagePayloadFactory;
        this.textMessagePayloadFactory = textMessagePayloadFactory;
        this.cycleDurationInDays = cycleDurationInDays;
        this.reportABirthMessageDelay = reportABirthMessageDelay;
    }

    public void sendReportClaimMessage(Claim claim, CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse, ClaimAction claimAction) {
        ReportClaimMessagePayload payload = buildReportClaimMessagePayload(claim, identityAndEligibilityResponse, claimAction, emptyList());
        messageQueueClient.sendMessage(payload, REPORT_CLAIM);
    }

    public void sendReportClaimMessageWithUpdatedClaimantFields(Claim claim,
                                                                CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse,
                                                                List<UpdatableClaimantField> updatedClaimantFields) {
        ReportClaimMessagePayload payload = buildReportClaimMessagePayload(claim, identityAndEligibilityResponse, ClaimAction.UPDATED, updatedClaimantFields);
        messageQueueClient.sendMessage(payload, REPORT_CLAIM);
    }

    public void sendAdditionalPaymentMessage(Claim claim) {
        AdditionalPregnancyPaymentMessagePayload payload = AdditionalPregnancyPaymentMessagePayload.withClaimId(claim.getId());
        messageQueueClient.sendMessage(payload, ADDITIONAL_PREGNANCY_PAYMENT);
    }

    public void sendNewCardMessage(Claim claim, EligibilityAndEntitlementDecision decision) {
        RequestNewCardMessagePayload payload = buildNewCardMessagePayload(claim, decision);
        messageQueueClient.sendMessage(payload, REQUEST_NEW_CARD);
    }

    public void sendInstantSuccessEmail(Claim claim, EligibilityAndEntitlementDecision decision, EmailType emailType) {
        LocalDate nextPaymentDate = claim.getClaimStatusTimestamp().toLocalDate().plusDays(cycleDurationInDays);
        EmailMessagePayload messagePayload = emailMessagePayloadFactory.buildEmailMessagePayload(
                claim, decision.getVoucherEntitlement(), nextPaymentDate, emailType);
        messageQueueClient.sendMessage(messagePayload, SEND_EMAIL);
    }

    public void sendReportABirthEmailMessage(Claim claim) {
        MessagePayload payload = buildEmailMessagePayloadWithFirstAndLastNameOnly(claim, REPORT_A_BIRTH_REMINDER);
        messageQueueClient.sendMessageWithDelay(payload, SEND_EMAIL, reportABirthMessageDelay);
    }

    public void sendInstantSuccessText(Claim claim, EligibilityAndEntitlementDecision decision, TextType textType) {
        TextMessagePayload messagePayload = textMessagePayloadFactory.buildTextMessagePayload(
                claim, decision.getVoucherEntitlement(), textType);
        messageQueueClient.sendMessage(messagePayload, SEND_TEXT);
    }

    public void sendDecisionPendingEmailMessage(Claim claim) {
        MessagePayload payload = buildEmailMessagePayloadWithFirstAndLastNameOnly(claim, PENDING_DECISION);
        messageQueueClient.sendMessage(payload, SEND_EMAIL);
    }

    public void sendLetterWithAddressOnlyMessage(Claim claim, LetterType letterType) {
        MessagePayload payload = buildLetterPayloadWithAddressOnly(claim, letterType);
        messageQueueClient.sendMessage(payload, SEND_LETTER);
    }

    public void sendLetterWithAddressAndPaymentFieldsMessage(Claim claim, EligibilityAndEntitlementDecision decision, LetterType letterType) {
        MessagePayload payload = buildLetterPayloadWithAddressAndPaymentFields(claim, decision, letterType);
        messageQueueClient.sendMessage(payload, SEND_LETTER);
    }
}
