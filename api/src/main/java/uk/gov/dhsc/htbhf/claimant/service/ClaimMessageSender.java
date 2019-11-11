package uk.gov.dhsc.htbhf.claimant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.*;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildReportClaimMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.ADDITIONAL_PREGNANCY_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REPORT_CLAIM;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;

/**
 * Responsible for sending messages related to claims (new or updated).
 */
@Component
public class ClaimMessageSender {

    private final MessageQueueClient messageQueueClient;
    private final EmailMessagePayloadFactory emailMessagePayloadFactory;
    private final Integer cycleDurationInDays;

    public ClaimMessageSender(MessageQueueClient messageQueueClient,
                              EmailMessagePayloadFactory emailMessagePayloadFactory,
                              @Value("${payment-cycle.cycle-duration-in-days}") Integer cycleDurationInDays) {
        this.messageQueueClient = messageQueueClient;
        this.emailMessagePayloadFactory = emailMessagePayloadFactory;
        this.cycleDurationInDays = cycleDurationInDays;
    }

    public void sendReportClaimMessage(Claim claim, List<LocalDate> datesOfBirthOfChildren, ClaimAction claimAction) {
        ReportClaimMessagePayload payload = buildReportClaimMessagePayload(claim, datesOfBirthOfChildren, claimAction, emptyList());
        messageQueueClient.sendMessage(payload, REPORT_CLAIM);
    }

    public void sendReportClaimMessageWithUpdatedClaimantFields(Claim claim,
                                                                List<LocalDate> datesOfBirthOfChildren,
                                                                List<UpdatableClaimantField> updatedClaimantFields) {
        ReportClaimMessagePayload payload = buildReportClaimMessagePayload(claim, datesOfBirthOfChildren, ClaimAction.UPDATED, updatedClaimantFields);
        messageQueueClient.sendMessage(payload, REPORT_CLAIM);
    }

    public void sendAdditionalPaymentMessage(Claim claim) {
        AdditionalPregnancyPaymentMessagePayload payload = AdditionalPregnancyPaymentMessagePayload.withClaimId(claim.getId());
        messageQueueClient.sendMessage(payload, ADDITIONAL_PREGNANCY_PAYMENT);
    }

    public void sendNewCardMessage(Claim claim, EligibilityAndEntitlementDecision decision) {
        NewCardRequestMessagePayload payload = buildNewCardMessagePayload(claim, decision.getVoucherEntitlement(), decision.getDateOfBirthOfChildren());
        messageQueueClient.sendMessage(payload, CREATE_NEW_CARD);
    }

    public void sendInstantSuccessEmailMessage(Claim claim, EligibilityAndEntitlementDecision decision) {
        LocalDate nextPaymentDate = claim.getClaimStatusTimestamp().toLocalDate().plusDays(cycleDurationInDays);
        EmailMessagePayload messagePayload = emailMessagePayloadFactory.buildEmailMessagePayload(
                claim, decision.getVoucherEntitlement(), nextPaymentDate, EmailType.INSTANT_SUCCESS);
        messageQueueClient.sendMessage(messagePayload, SEND_EMAIL);
    }
}
