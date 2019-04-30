package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PaymentCycleVoucherEntitlementTest {

    @Test
    void shouldThrowExceptionWhenListIsNull() {
        IllegalArgumentException thrown = catchThrowableOfType(() -> new PaymentCycleVoucherEntitlement(null), IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("List of voucher entitlements must not be null or empty.");
    }

    @Test
    void shouldThrowExceptionWhenListIsEmpty() {
        IllegalArgumentException thrown = catchThrowableOfType(() -> new PaymentCycleVoucherEntitlement(emptyList()), IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("List of voucher entitlements must not be null or empty.");
    }

    @Test
    void shouldCreateTotalValuesFromListOfVoucherEntitlements() {
        VoucherEntitlement voucherEntitlement1 = VoucherEntitlement.builder()
                .vouchersForChildrenUnderOne(1)
                .vouchersForChildrenBetweenOneAndFour(2)
                .vouchersForPregnancy(3)
                .voucherValueInPence(100)
                .build();
        VoucherEntitlement voucherEntitlement2 = VoucherEntitlement.builder()
                .vouchersForChildrenUnderOne(2)
                .vouchersForChildrenBetweenOneAndFour(3)
                .vouchersForPregnancy(3)
                .voucherValueInPence(100)
                .build();

        PaymentCycleVoucherEntitlement result = new PaymentCycleVoucherEntitlement(asList(voucherEntitlement1, voucherEntitlement2));

        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(3);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(5);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(6);
        assertThat(result.getVoucherValueInPence()).isEqualTo(100);
        assertThat(result.getTotalVoucherEntitlement())
                .isEqualTo(voucherEntitlement1.getTotalVoucherEntitlement() + voucherEntitlement2.getTotalVoucherEntitlement());
        assertThat(result.getTotalVoucherValueInPence())
                .isEqualTo(voucherEntitlement1.getTotalVoucherValueInPence() + voucherEntitlement2.getTotalVoucherValueInPence());
    }

    @Test
    void shouldCreateTotalValuesFromListOfVoucherEntitlementWithMissingValues() {
        VoucherEntitlement voucherEntitlement1 = VoucherEntitlement.builder()
                .vouchersForChildrenUnderOne(1)
                .voucherValueInPence(100)
                .build();
        VoucherEntitlement voucherEntitlement2 = VoucherEntitlement.builder()
                .vouchersForChildrenBetweenOneAndFour(1)
                .voucherValueInPence(100)
                .build();

        PaymentCycleVoucherEntitlement result = new PaymentCycleVoucherEntitlement(asList(voucherEntitlement1, voucherEntitlement2));

        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(1);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(1);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0);
        assertThat(result.getVoucherValueInPence()).isEqualTo(100);
        assertThat(result.getTotalVoucherEntitlement())
                .isEqualTo(voucherEntitlement1.getTotalVoucherEntitlement() + voucherEntitlement2.getTotalVoucherEntitlement());
        assertThat(result.getTotalVoucherValueInPence())
                .isEqualTo(voucherEntitlement1.getTotalVoucherValueInPence() + voucherEntitlement2.getTotalVoucherValueInPence());
    }

}
