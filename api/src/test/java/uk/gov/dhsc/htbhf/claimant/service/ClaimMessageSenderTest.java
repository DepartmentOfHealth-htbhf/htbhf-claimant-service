package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.AdditionalPregnancyPaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.ReportClaimMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
