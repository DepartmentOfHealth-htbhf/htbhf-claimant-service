package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.ReportClaimMessagePayload;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildReportClaimMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;

class MessagePayloadFactoryTest {

    @Test
    void shouldCreateNewCardMessagePayload() {
        Claim claim = aValidClaim();
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        List<LocalDate> datesOfBirth = List.of(LocalDate.now().minusDays(1), LocalDate.now());
        NewCardRequestMessagePayload payload = MessagePayloadFactory.buildNewCardMessagePayload(claim, voucherEntitlement, datesOfBirth);

        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getVoucherEntitlement()).isEqualTo(voucherEntitlement);
        assertThat(payload.getDatesOfBirthOfChildren()).isEqualTo(datesOfBirth);
    }

    @Test
    void shouldCreateMakePaymentMessagePayload() {
        PaymentCycle paymentCycle = aValidPaymentCycle();

        MakePaymentMessagePayload payload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(payload.getCardAccountId()).isEqualTo(paymentCycle.getClaim().getCardAccountId());
    }

    @Test
    void shouldBuildReportClaimMessagePayload() {
        Claim claim = aValidClaim();
        List<LocalDate> datesOfBirthOfChildren = asList(LocalDate.now().minusYears(1), LocalDate.now().minusYears(2));
        ClaimAction claimAction = ClaimAction.NEW;
        LocalDateTime now = LocalDateTime.now();

        ReportClaimMessagePayload payload = buildReportClaimMessagePayload(claim, datesOfBirthOfChildren, claimAction);

        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getDatesOfBirthOfChildren()).isEqualTo(datesOfBirthOfChildren);
        assertThat(payload.getClaimAction()).isEqualTo(claimAction);
        assertThat(payload.getTimestamp()).isAfterOrEqualTo(now);
    }
}
