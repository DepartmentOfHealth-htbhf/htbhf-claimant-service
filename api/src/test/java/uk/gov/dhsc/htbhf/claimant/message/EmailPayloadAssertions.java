package uk.gov.dhsc.htbhf.claimant.message;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_LAST_NAME;

public class EmailPayloadAssertions {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * The values asserted in this method are for a claimant who has pregnancy vouchers only in their entitlement
     * which can be built using aVoucherEntitlementWithPregnancyVoucherOnlyForDate on
     * {@link uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory}.
     *
     * @param emailPersonalisation The email template values to verify
     * @param nextPaymentDate      The next payment date to assert
     */
    public static void assertEmailPayloadCorrectForClaimantWithPregnancyVouchersOnly(Map<String, Object> emailPersonalisation, LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£12.40"),
                entry("pregnancy_payment", "\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", ""),
                entry("children_under_4_payment", ""),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate))
        );
    }

    /**
     * Asserts that the values are correct for an email where we notify the claimant that their entitlement will be changing
     * because one or more of their children will be turning 4 in the next payment cycle.
     *
     * @param emailPersonalisation The map of data to verify
     * @param nextPaymentDate      The payment date expected in the Map.
     */
    public static void assertEmailPayloadCorrectForChildUnderFourNotificationWithPregnancyVouchers(Map<String, Object> emailPersonalisation,
                                                                                                   LocalDate nextPaymentDate) {
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£37.20"),
                entry("pregnancy_payment", "\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", ""),
                entry("multiple_children", false),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate))
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
        assertThat(emailPersonalisation).containsOnly(
                entry("First_name", VALID_FIRST_NAME),
                entry("Last_name", VALID_LAST_NAME),
                entry("payment_amount", "£49.60"),
                entry("pregnancy_payment", "\n* £12.40 for a pregnancy"),
                entry("children_under_1_payment", "\n* £24.80 for children under 1"),
                entry("children_under_4_payment", "\n* £12.40 for children between 1 and 4"),
                entry("next_payment_date", DATE_FORMATTER.format(nextPaymentDate))
        );
    }
}
