package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherEntitlementTest {

    @Test
    void shouldSetTheCorrectEntitlementAndValueTotals() {
        VoucherEntitlement entitlement = VoucherEntitlement.builder()
                .vouchersForPregnancy(2)
                .vouchersForChildrenUnderOne(4)
                .vouchersForChildrenBetweenOneAndFour(3)
                .voucherValue(new BigDecimal("3.10"))
                .build();

        assertThat(entitlement.getVouchersForPregnancy()).isEqualTo(2);
        assertThat(entitlement.getVouchersForChildrenUnderOne()).isEqualTo(4);
        assertThat(entitlement.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(3);
        assertThat(entitlement.getVoucherValue()).isEqualTo(new BigDecimal("3.10"));
        assertThat(entitlement.getTotalVoucherEntitlement()).isEqualTo(9);
        assertThat(entitlement.getTotalVoucherValue()).isEqualTo(new BigDecimal("27.90"));
    }
}