package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.formatVoucherAmount;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_LAST_NAME;

public class EmailPayloadAssertions {

    public static final DateTimeFormatter EMAIL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * The values asserted in this method are for a claimant who has pregnancy vouchers only in their entitlement
     * which can be built using aVoucherEntitlementWithPregnancyVoucherOnlyForDate on
     * {@link uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory}.
     *
     * @param emailPersonalisation The email template values to verify
     * @param nextPaymentDate      The next payment date to assert
     */
    public static void assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly(Map<String, Object> emailPersonalisation, LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).contains(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£12.40"),
                entry("pregnancy_payment", "\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", ""),
                entry("children_under_4_payment", ""),
                entry("next_payment_date", EMAIL_DATE_FORMATTER.format(nextPaymentDate))
        );
    }

    /**
     * The values asserted in this method are for a claimant who has vouchers for pregnancy, under 1 and between 1 and 4 children only in their entitlement
     * which can be built using aValidVoucherEntitlement on {@link uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory}.
     *
     * @param emailPersonalisation The email template values to verify
     * @param nextPaymentDate      The next payment date to assert
     */
    public static void assertEmailPayloadCorrectForClaimantWithAllVouchers(Map<String, Object> emailPersonalisation, LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).contains(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£49.60"),
                entry("pregnancy_payment", "\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", "\n* £12.40 for children between 1 and 4"),
                entry("next_payment_date", EMAIL_DATE_FORMATTER.format(nextPaymentDate))
        );
    }

    public static void assertThatEmailPayloadCorrectForBackdatedPayment(Map<String, Object> emailPersonalisation, PaymentCycle paymentCycle) {
        Claim claim = paymentCycle.getClaim();
        PaymentCycleVoucherEntitlement voucherEntitlement = paymentCycle.getVoucherEntitlement();
        assertThat(emailPersonalisation).contains(
                entry("First_name", claim.getClaimant().getFirstName()),
                entry("Last_name", claim.getClaimant().getLastName()),
                entry("payment_amount", formatVoucherAmount(paymentCycle.getTotalVouchers())),
                entry("pregnancy_payment", ""),
                entry("children_under_1_payment",
                        formatListEntry(voucherEntitlement.getVouchersForChildrenUnderOne(), "for children under 1")),
                entry("children_under_4_payment",
                        formatListEntry(voucherEntitlement.getVouchersForChildrenBetweenOneAndFour(), "for children between 1 and 4")),
                entry("backdated_amount", formatVoucherAmount(voucherEntitlement.getBackdatedVouchers())),
                entry("next_payment_date", EMAIL_DATE_FORMATTER.format(paymentCycle.getCycleEndDate().plusDays(1))),
                entry("regular_payment", formatVoucherAmount(voucherEntitlement.getLastVoucherEntitlementForCycle().getTotalVoucherEntitlement() * 4))
        );
    }

    private static String formatListEntry(int voucherCount, String voucherReason) {
        String voucherAmount = formatVoucherAmount(voucherCount);
        return isEmpty(voucherAmount) ? "" : "\n* " + voucherAmount + " " + voucherReason;
    }
}
