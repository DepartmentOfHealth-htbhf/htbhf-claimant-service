package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHER_VALUE_IN_PENCE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;

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
                .singleVoucherValueInPence(100)
                .build();
        VoucherEntitlement voucherEntitlement2 = VoucherEntitlement.builder()
                .vouchersForChildrenUnderOne(2)
                .vouchersForChildrenBetweenOneAndFour(3)
                .vouchersForPregnancy(3)
                .singleVoucherValueInPence(100)
                .build();
        List<VoucherEntitlement> voucherEntitlements = asList(voucherEntitlement1, voucherEntitlement2);

        PaymentCycleVoucherEntitlement result = new PaymentCycleVoucherEntitlement(voucherEntitlements);

        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(3);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(5);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(6);
        assertThat(result.getSingleVoucherValueInPence()).isEqualTo(100);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(14);
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(1400);
        assertThat(result.getVoucherEntitlements()).isEqualTo(voucherEntitlements);
        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
    }

    @Test
    void shouldCreateTotalValuesFromListOfVoucherEntitlementWithMissingValues() {
        VoucherEntitlement voucherEntitlement1 = VoucherEntitlement.builder()
                .vouchersForChildrenUnderOne(1)
                .singleVoucherValueInPence(100)
                .build();
        VoucherEntitlement voucherEntitlement2 = VoucherEntitlement.builder()
                .vouchersForChildrenBetweenOneAndFour(1)
                .singleVoucherValueInPence(100)
                .build();
        List<VoucherEntitlement> voucherEntitlements = asList(voucherEntitlement1, voucherEntitlement2);

        PaymentCycleVoucherEntitlement result = new PaymentCycleVoucherEntitlement(voucherEntitlements);

        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(1);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(1);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0);
        assertThat(result.getSingleVoucherValueInPence()).isEqualTo(100);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(2);
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(200);
        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
        assertThat(result.getVoucherEntitlements()).isEqualTo(voucherEntitlements);
    }

    @Test
    void shouldCreateTotalValuesFromListOfVoucherEntitlementsAndNumberOfBackDatedVouchers() {
        VoucherEntitlement voucherEntitlement1 = VoucherEntitlement.builder()
                .vouchersForChildrenUnderOne(1)
                .vouchersForChildrenBetweenOneAndFour(2)
                .vouchersForPregnancy(3)
                .singleVoucherValueInPence(100)
                .build();
        VoucherEntitlement voucherEntitlement2 = VoucherEntitlement.builder()
                .vouchersForChildrenUnderOne(2)
                .vouchersForChildrenBetweenOneAndFour(3)
                .vouchersForPregnancy(3)
                .singleVoucherValueInPence(100)
                .build();
        List<VoucherEntitlement> voucherEntitlements = asList(voucherEntitlement1, voucherEntitlement2);
        int backdatedVouchers = 10;

        PaymentCycleVoucherEntitlement result = new PaymentCycleVoucherEntitlement(voucherEntitlements, backdatedVouchers);

        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(3);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(5);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(6);
        assertThat(result.getVoucherEntitlements()).isEqualTo(voucherEntitlements);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(24);
        assertThat(result.getSingleVoucherValueInPence()).isEqualTo(100);
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(2400);
        assertThat(result.getBackdatedVouchers()).isEqualTo(backdatedVouchers);
    }

    @Test
    void shouldReturnEarliestVoucherEntitlement() {
        LocalDate minusOneWeek = LocalDate.now().minusWeeks(1);
        LocalDate minusTwoWeeks = LocalDate.now().minusWeeks(2);
        LocalDate minusThreeWeeks = LocalDate.now().minusWeeks(3);
        LocalDate minusFourWeeks = LocalDate.now().minusWeeks(4);
        List<VoucherEntitlement> entitlements = List.of(
                aVoucherEntitlementWithEntitlementDate(minusFourWeeks),
                aVoucherEntitlementWithEntitlementDate(minusTwoWeeks),
                aVoucherEntitlementWithEntitlementDate(minusOneWeek),
                aVoucherEntitlementWithEntitlementDate(minusThreeWeeks)
        );
        PaymentCycleVoucherEntitlement entitlement = PaymentCycleVoucherEntitlement.builder()
                .voucherEntitlements(entitlements)
                .build();

        LocalDate earliestVoucherEntitlementDate = entitlement.getFirstVoucherEntitlementForCycle().getEntitlementDate();
        assertThat(earliestVoucherEntitlementDate).isEqualTo(minusFourWeeks);
    }

    @Test
    void shouldReturnLastVoucherEntitlement() {
        LocalDate minusOneWeek = LocalDate.now().minusWeeks(1);
        LocalDate minusTwoWeeks = LocalDate.now().minusWeeks(2);
        LocalDate minusThreeWeeks = LocalDate.now().minusWeeks(3);
        LocalDate minusFourWeeks = LocalDate.now().minusWeeks(4);
        List<VoucherEntitlement> entitlements = List.of(
                aVoucherEntitlementWithEntitlementDate(minusFourWeeks),
                aVoucherEntitlementWithEntitlementDate(minusTwoWeeks),
                aVoucherEntitlementWithEntitlementDate(minusOneWeek),
                aVoucherEntitlementWithEntitlementDate(minusThreeWeeks)
        );
        PaymentCycleVoucherEntitlement entitlement = PaymentCycleVoucherEntitlement.builder()
                .voucherEntitlements(entitlements)
                .build();

        LocalDate lastVoucherEntitlement = entitlement.getLastVoucherEntitlementForCycle().getEntitlementDate();
        assertThat(lastVoucherEntitlement).isEqualTo(minusOneWeek);
    }

    @Test
    void shouldReturnCorrectBackdatedVouchersValue() {
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        int backdatedVouchers = 10;

        PaymentCycleVoucherEntitlement result = new PaymentCycleVoucherEntitlement(asList(voucherEntitlement), backdatedVouchers);

        assertThat(result.getBackdatedVouchersValueInPence()).isEqualTo(backdatedVouchers * VOUCHER_VALUE_IN_PENCE);
    }

}
