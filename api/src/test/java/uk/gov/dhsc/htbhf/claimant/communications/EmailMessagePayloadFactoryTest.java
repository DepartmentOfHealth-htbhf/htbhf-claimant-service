package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import java.time.LocalDate;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithAllVouchers;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertThatEmailPayloadCorrectForBackdatedPayment;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleEntitlementAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithPregnancyVouchersOnly;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartAndEndDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild;

class EmailMessagePayloadFactoryTest {

    private EmailMessagePayloadFactory emailMessagePayloadFactory = new EmailMessagePayloadFactory();

    @Test
    void shouldBuildSendNewCardSuccessEmailPayloadWithAllPaymentTypes() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(startDate, endDate);

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.NEW_CARD);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.NEW_CARD);
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildSendNewCardSuccessEmailPayloadWithOnlyPregnancyPayment() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithPregnancyVouchersOnly(startDate, endDate);

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.NEW_CARD);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.NEW_CARD);
        assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildPaymentNotificationEmailPayloadWithAllPaymentTypes() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(startDate, endDate);

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.PAYMENT);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.PAYMENT);
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildPaymentNotificationEmailPayloadWithOnlyPregnancyPayment() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithPregnancyVouchersOnly(startDate, endDate);

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.PAYMENT);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.PAYMENT);
        assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildPaymentNotificationEmailPayloadWithBackdatedVouchers() {
        Claim claim = aClaimWithExpectedDeliveryDate(LocalDate.now().minusWeeks(8));
        PaymentCycleVoucherEntitlement voucherEntitlement =
                aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild(LocalDate.now(), asList(LocalDate.now().minusWeeks(6)));
        PaymentCycle paymentCycle = aPaymentCycleWithCycleEntitlementAndClaim(voucherEntitlement, claim);

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.NEW_CHILD_FROM_PREGNANCY);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.NEW_CHILD_FROM_PREGNANCY);
        assertThatEmailPayloadCorrectForBackdatedPayment(payload.getEmailPersonalisation(), paymentCycle);
    }
}
