package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForChildUnderFourNotificationWithPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithAllVouchers;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithPregnancyVouchersOnly;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartAndEndDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchersForUnderOnePlusOneForFirstWeekForUnderFour;

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
    void shouldBuildSendNewCardSuccessEmailPayloadWithAllPaymentTypes() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(startDate, endDate);

        EmailMessagePayload payload = MessagePayloadFactory.buildSendNewCardSuccessEmailPayload(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.NEW_CARD);
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildSendNewCardSuccessEmailPayloadWithOnlyPregnancyPayment() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithPregnancyVouchersOnly(startDate, endDate);

        EmailMessagePayload payload = MessagePayloadFactory.buildSendNewCardSuccessEmailPayload(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.NEW_CARD);
        assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildPaymentNotificationEmailPayloadWithAllPaymentTypes() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(startDate, endDate);

        EmailMessagePayload payload = MessagePayloadFactory.buildPaymentNotificationEmailPayload(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.PAYMENT);
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildPaymentNotificationEmailPayloadWithOnlyPregnancyPayment() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithPregnancyVouchersOnly(startDate, endDate);

        EmailMessagePayload payload = MessagePayloadFactory.buildPaymentNotificationEmailPayload(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.PAYMENT);
        assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildChildTurnsFourNotificationEmailPayload() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(startDate, endDate);
        //This is specifically built so that it is different to the voucher entitlement on the PaymentCycle - it has no vouchers for under 4
        PaymentCycleVoucherEntitlement paymentCycleVoucherEntitlement =
                aPaymentCycleVoucherEntitlementWithVouchersForUnderOnePlusOneForFirstWeekForUnderFour(startDate);
        boolean multipleChildrenTurningFourInMonth = false;

        EmailMessagePayload payload = MessagePayloadFactory.buildChildTurnsFourNotificationEmailPayload(paymentCycle,
                paymentCycleVoucherEntitlement, multipleChildrenTurningFourInMonth);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertEmailPayloadCorrectForChildUnderFourNotificationWithPregnancyVouchers(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }
}
