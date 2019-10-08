package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.AdditionalPregnancyPaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.ReportClaimMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;

import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.ADDITIONAL_PREGNANCY_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REPORT_CLAIM;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@ExtendWith(MockitoExtension.class)
class ClaimMessageSenderTest {

    @Mock
    private MessageQueueClient messageQueueClient;

    @InjectMocks
    private ClaimMessageSender claimMessageSender;

    @Test
    void shouldSendReportClaimMessage() {
        Claim claim = aValidClaim();

        claimMessageSender.sendReportClaimMessage(claim);

        ReportClaimMessagePayload expectedPayload = ReportClaimMessagePayload.builder()
                .claimId(claim.getId())
                .build();
        verify(messageQueueClient).sendMessage(expectedPayload, REPORT_CLAIM);
    }

    @Test
    void shouldSendNewCardMessage() {
        Claim claim = aValidClaim();
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);

        claimMessageSender.sendNewCardMessage(claim, decision);

        NewCardRequestMessagePayload newCardRequestMessagePayload = NewCardRequestMessagePayload.builder()
                .claimId(claim.getId())
                .voucherEntitlement(decision.getVoucherEntitlement())
                .datesOfBirthOfChildren(decision.getDateOfBirthOfChildren())
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

}
