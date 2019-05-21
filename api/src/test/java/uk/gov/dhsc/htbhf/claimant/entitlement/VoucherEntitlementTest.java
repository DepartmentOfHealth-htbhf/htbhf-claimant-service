package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherEntitlementTest {

    @Test
    void shouldSetTheCorrectEntitlementAndValueTotals() {
        LocalDate entitlementDate = LocalDate.now();
        VoucherEntitlement entitlement = VoucherEntitlement.builder()
                .vouchersForPregnancy(2)
                .vouchersForChildrenUnderOne(4)
                .vouchersForChildrenBetweenOneAndFour(3)
                .singleVoucherValueInPence(310)
                .entitlementDate(entitlementDate)
                .build();

        assertThat(entitlement.getVouchersForPregnancy()).isEqualTo(2);
        assertThat(entitlement.getVouchersForChildrenUnderOne()).isEqualTo(4);
        assertThat(entitlement.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(3);
        assertThat(entitlement.getSingleVoucherValueInPence()).isEqualTo(310);
        assertThat(entitlement.getTotalVoucherEntitlement()).isEqualTo(9);
        assertThat(entitlement.getTotalVoucherValueInPence()).isEqualTo(2790);
        assertThat(entitlement.getEntitlementDate()).isEqualTo(entitlementDate);
    }
}
