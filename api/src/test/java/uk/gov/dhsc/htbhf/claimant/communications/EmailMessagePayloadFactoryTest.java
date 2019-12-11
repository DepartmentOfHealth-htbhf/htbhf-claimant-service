package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithAllVouchers;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertThatEmailPayloadCorrectForBackdatedPayment;
import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.LAST_NAME;
import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.REGULAR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.CARD_IS_ABOUT_TO_BE_CANCELLED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDateAndChildrenDobs;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleEntitlementAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithPregnancyVouchersOnly;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartAndEndDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild;

class EmailMessagePayloadFactoryTest {

    private static final Integer NUMBER_OF_CALCULATION_PERIODS = 4;

    private EmailMessagePayloadFactory emailMessagePayloadFactory = new EmailMessagePayloadFactory(NUMBER_OF_CALCULATION_PERIODS);

    @Test
    void shouldBuildEmailMessagePayloadWithAllVouchers() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(startDate, endDate);

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.INSTANT_SUCCESS);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.INSTANT_SUCCESS);
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildEmailPayloadWithOnlyPregnancyPayment() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithPregnancyVouchersOnly(startDate, endDate);

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.INSTANT_SUCCESS);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.INSTANT_SUCCESS);
        assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly(payload.getEmailPersonalisation(), endDate.plusDays(1));
    }

    @Test
    void shouldBuildPaymentNotificationEmailPayloadWithAllVouchers() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(startDate, endDate);

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.REGULAR_PAYMENT);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.REGULAR_PAYMENT);
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), endDate.plusDays(1));
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

    @Test
    void shouldCreateEmailPersonalisationWithFirstAndLastName() {
        Claimant claimant = aValidClaimant();

        Map<String, Object> emailPersonalisation = EmailMessagePayloadFactory.createEmailPersonalisationWithFirstAndLastNameOnly(claimant);

        assertThat(emailPersonalisation).containsOnly(
                entry(FIRST_NAME.getTemplateKeyName(), claimant.getFirstName()),
                entry(LAST_NAME.getTemplateKeyName(), claimant.getLastName())
        );
    }

    @Test
    void shouldBuildEmailMessagePayloadWithFirstAndLastName() {
        Claim claim = aValidClaim();

        EmailMessagePayload payload = EmailMessagePayloadFactory.buildEmailMessagePayloadWithFirstAndLastNameOnly(claim, CARD_IS_ABOUT_TO_BE_CANCELLED);

        assertThat(payload.getEmailPersonalisation()).containsOnly(
                entry(FIRST_NAME.getTemplateKeyName(), claim.getClaimant().getFirstName()),
                entry(LAST_NAME.getTemplateKeyName(), claim.getClaimant().getLastName()));
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getEmailType()).isEqualTo(CARD_IS_ABOUT_TO_BE_CANCELLED);
    }


    @Test
    void shouldBuildEmailMessageWithZeroAmountForNextCycle() {
        Claim claim = aClaimWithExpectedDeliveryDateAndChildrenDobs(LocalDate.now().minusYears(4), List.of(LocalDate.now().minusYears(4).plusDays(1)));
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .claim(claim)
                .identityAndEligibilityResponse(CombinedIdAndEligibilityResponseTestDataFactory
                        .anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(claim.getClaimant().getChildrenDob()))
                .voucherEntitlement(aPaymentCycleVoucherEntitlementMatchingChildren(LocalDate.now(), claim.getClaimant().getChildrenDob()))
                .build();

        EmailMessagePayload payload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.CHILD_TURNS_FOUR);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getEmailPersonalisation()).contains(entry(REGULAR_PAYMENT.getTemplateKeyName(), "£0.00"));
    }

}
