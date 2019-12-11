package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.ReportClaimMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.RequestNewCardMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildReportClaimMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

class MessagePayloadFactoryTest {

    @Test
    void shouldCreateNewCardMessagePayload() {
        Claim claim = aValidClaim();
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = anEligibleDecision();
        RequestNewCardMessagePayload payload = MessagePayloadFactory.buildNewCardMessagePayload(claim, eligibilityAndEntitlementDecision);

        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getEligibilityAndEntitlementDecision()).isEqualTo(eligibilityAndEntitlementDecision);
    }

    @Test
    void shouldCreateMakePaymentMessagePayload() {
        PaymentCycle paymentCycle = aValidPaymentCycle();

        MakePaymentMessagePayload payload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(payload.getCardAccountId()).isEqualTo(paymentCycle.getClaim().getCardAccountId());
        assertThat(payload.isPaymentRestarted()).isFalse();
    }

    @Test
    void shouldCreateMakePaymentMessagePayloadForRestartedPayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();

        MakePaymentMessagePayload payload = MessagePayloadFactory.buildMakePaymentMessagePayloadForRestartedPayment(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(payload.getCardAccountId()).isEqualTo(paymentCycle.getClaim().getCardAccountId());
        assertThat(payload.isPaymentRestarted()).isTrue();
    }

    @Test
    void shouldBuildReportClaimMessagePayload() {
        Claim claim = aValidClaim();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        ClaimAction claimAction = ClaimAction.NEW;
        LocalDateTime now = LocalDateTime.now();
        List<UpdatableClaimantField> updatedClaimantFields = List.of(FIRST_NAME);

        ReportClaimMessagePayload payload = buildReportClaimMessagePayload(claim, identityAndEligibilityResponse, claimAction, updatedClaimantFields);

        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getIdentityAndEligibilityResponse()).isEqualTo(identityAndEligibilityResponse);
        assertThat(payload.getClaimAction()).isEqualTo(claimAction);
        assertThat(payload.getTimestamp()).isAfterOrEqualTo(now);
        assertThat(payload.getUpdatedClaimantFields()).isEqualTo(updatedClaimantFields);
    }
}
