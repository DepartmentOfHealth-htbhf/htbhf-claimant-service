package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CycleEntitlementCalculatorIntegrationTests {

    @Autowired
    private CycleEntitlementCalculator cycleEntitlementCalculator;

    @DisplayName("Should calculate the correct entitlement when there is a child under one,"
             + " a child under four and neither child changes age during the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForNoChanges() {
        LocalDate firstBirthdayInSixMonths = LocalDate.now().minusMonths(6);
        LocalDate fourthBirthdayInOneYear = LocalDate.now().minusYears(3);
        List<LocalDate> childrenDatesOfBirth = asList(firstBirthdayInSixMonths, fourthBirthdayInOneYear);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(Optional.empty(), childrenDatesOfBirth);

        // Calculation per week: pregnant = 0 vouchers, children under 1 = 2 vouchers, children under 4 = 1 voucher
        // Total per week = 3 vouchers. Total over 4 weeks = 12 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 3720 pence
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(3720);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(12);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(4);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    @DisplayName("Should calculate the correct entitlement for a cycle when there is a child under one, whose first birthday"
             + " is on day two of week one of the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForEntitlementChanges1() {
        LocalDate firstBirthdayOnDayTwoOfWeekOne = LocalDate.now().minusYears(1).plusDays(1);
        List<LocalDate> childrenDatesOfBirth = singletonList(firstBirthdayOnDayTwoOfWeekOne);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(Optional.empty(), childrenDatesOfBirth);

        // Calculation first week: pregnant = 0 voucher, children under 1 = 2 vouchers, children under 4 = 0 vouchers
        // Calculation subsequent weeks: pregnant = 0 voucher, children under 1 = 0 vouchers, children under 4 = 1 voucher
        // Total = 5 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 1550 pence
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(1550);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(5);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(2);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(3);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    @DisplayName("Should calculate the correct entitlement for a cycle when the claimant is pregnant and there is a child between"
             + " one and four whose fourth birthday is on day two of week two of the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForEntitlementChanges2() {
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().plusMonths(1));
        LocalDate fourthBirthdayOnDayTwoOfWeekTwo = LocalDate.now().minusYears(4).plusWeeks(1).plusDays(1);
        List<LocalDate> childrenDatesOfBirth = singletonList(fourthBirthdayOnDayTwoOfWeekTwo);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth);

        // Calculation first and second week: pregnant = 1 voucher, children under 1 = 0 vouchers, children under 4 = 1 voucher
        // Calculation subsequent weeks: pregnant = 1 voucher, children under 1 = 0 vouchers, children under 4 = 0 vouchers
        // Total = 6 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 1860 pence
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(1860);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(6);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(0);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(2);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(4);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    @DisplayName("Should calculate the correct entitlement for a cycle when there is one child under one whose first birthday is on"
             + " day two of week three, and one child between one and four whose fourth birthday is on day one of week four of the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForEntitlementChanges3() {
        LocalDate firstBirthdayOnDayTwoOfWeekThree = LocalDate.now().minusYears(1).plusWeeks(2).plusDays(1);
        LocalDate fourthBirthdayOnDayOneOfWeekFour = LocalDate.now().minusYears(4).plusWeeks(3);
        List<LocalDate> childrenDatesOfBirth = asList(firstBirthdayOnDayTwoOfWeekThree, fourthBirthdayOnDayOneOfWeekFour);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(Optional.empty(), childrenDatesOfBirth);

        // Calculation first three weeks: pregnant = 0 vouchers, children under 1 = 2 vouchers, children under 4 = 1 voucher
        // Calculation last week: pregnant = 0 vouchers, children under 1 = 0 vouchers, children under 4 = 1 vouchers
        // Total = 10 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 3100 pence
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(3100);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(10);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(6);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(4);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    @DisplayName("Should calculate the correct entitlement for a cycle when there is one child under one whose first birthday is on"
             + " day two of week three, and one child between one and four whose fourth birthday is on day two of week four of the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForEntitlementChangesOne4() {
        LocalDate firstBirthdayOnDayTwoOfWeekThree = LocalDate.now().minusYears(1).plusWeeks(2).plusDays(1);
        LocalDate fourthBirthdayOnDayTwoOfWeekFour = LocalDate.now().minusYears(4).plusWeeks(3).plusDays(1);
        List<LocalDate> childrenDatesOfBirth = asList(firstBirthdayOnDayTwoOfWeekThree, fourthBirthdayOnDayTwoOfWeekFour);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(Optional.empty(), childrenDatesOfBirth);

        // Calculation first three weeks: pregnant = 0 vouchers, children under 1 = 2 vouchers, children under 4 = 1 voucher
        // Calculation last week: pregnant = 0 vouchers, children under 1 = 0 vouchers, children under 4 = 2 vouchers
        // Total = 11 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 3410 pence
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(3410);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(11);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(6);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(5);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    @Test
    void shouldRemoveVoucherForPregnancyWhenANewChildUnderOneMatchedToPregnancy() {
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().vouchersForPregnancy(1).build();
        PaymentCycleVoucherEntitlement previousEntitlement = new PaymentCycleVoucherEntitlement(singletonList(voucherEntitlement));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().minusWeeks(2));
        LocalDate bornLastWeek = LocalDate.now().minusWeeks(1);
        List<LocalDate> childrenDatesOfBirth = singletonList(bornLastWeek);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, previousEntitlement);

        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(2480);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(0);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0); // we've determine that the child is from the pregnancy, so no longer pregnant
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    @Test
    void shouldRemoveVoucherForPregnancyWhenTwoNewChildrenBornOneDayApartUnderOneMatchedToPregnancy() {
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().vouchersForPregnancy(1).build();
        PaymentCycleVoucherEntitlement previousEntitlement = new PaymentCycleVoucherEntitlement(singletonList(voucherEntitlement));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().minusWeeks(2));
        LocalDate bornLastWeek = LocalDate.now().minusWeeks(1);
        LocalDate bornEightDaysAgo = LocalDate.now().minusWeeks(1).minusDays(1);
        // date of births representing twins born one day apart
        List<LocalDate> childrenDatesOfBirth = asList(bornLastWeek, bornEightDaysAgo);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, previousEntitlement);

        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(4960);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(16);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(16);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(0);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0); // we've determine that the children are from the pregnancy, so no longer pregnant
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    @Test
    void shouldCalculateNewEntitlementWhenGivenAPreviousEntitlement() {
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().build();
        PaymentCycleVoucherEntitlement previousEntitlement = new PaymentCycleVoucherEntitlement(singletonList(voucherEntitlement));
        Optional<LocalDate> expectedDueDate = Optional.empty();
        LocalDate bornLastWeek = LocalDate.now().minusWeeks(1);
        List<LocalDate> childrenDatesOfBirth = singletonList(bornLastWeek);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, previousEntitlement);

        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(2480);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(0);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    // Covers the case of an existing claimant becoming pregnant
    @Test
    void shouldCalculateEntitlementForPregnantClaimantWithNoPreviousPregnancyVouchers() {
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().build();
        PaymentCycleVoucherEntitlement previousEntitlement = new PaymentCycleVoucherEntitlement(singletonList(voucherEntitlement));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().plusMonths(8));
        LocalDate threeYearsOld = LocalDate.now().minusYears(3);
        List<LocalDate> childrenDatesOfBirth = singletonList(threeYearsOld);

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, previousEntitlement);

        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(2480);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(0);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(4);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(4);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }
}
