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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithFourWeeklyPregnancyVouchersOnly;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_LAST_NAME;

class MessagePayloadFactoryTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

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
        //TODO MRS 2019-07-25: This a realistic PaymentCycle with 4 weekly entitlements in it, change the default constructed PaymentCycle with entitlements
        // so it is more realistic like this, plus remove those PaymentCycles that no longer make sense from the TestDataFactory.
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .cycleStartDate(startDate)
                .cycleEndDate(endDate)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers())
                .build();

        EmailMessagePayload payload = MessagePayloadFactory.buildSendNewCardSuccessEmailPayload(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.NEW_CARD);
        Map<String, Object> emailPersonalisation = payload.getEmailPersonalisation();

        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("first_payment_amount", "£49.60"),
                entry("pregnancy_payment", "\\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", "\\n* £24.80 for children under 1"),
                entry("children_under_4_payment", "\\n* £12.40 for children between 1 and 4"),
                entry("next_payment_date", DATE_FORMATTER.format(endDate))
        );
    }

    @Test
    void shouldBuildSendNewCardSuccessEmailPayloadWithOnlyPregnancyPayment() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithFourWeeklyPregnancyVouchersOnly())
                .cycleStartDate(startDate)
                .cycleEndDate(endDate)
                .totalEntitlementAmountInPence(1240)
                .build();

        EmailMessagePayload payload = MessagePayloadFactory.buildSendNewCardSuccessEmailPayload(paymentCycle);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.NEW_CARD);
        Map<String, Object> emailPersonalisation = payload.getEmailPersonalisation();

        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("first_payment_amount", "£12.40"),
                entry("pregnancy_payment", "\\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", ""),
                entry("children_under_4_payment", ""),
                entry("next_payment_date", DATE_FORMATTER.format(endDate))
        );
    }

}
