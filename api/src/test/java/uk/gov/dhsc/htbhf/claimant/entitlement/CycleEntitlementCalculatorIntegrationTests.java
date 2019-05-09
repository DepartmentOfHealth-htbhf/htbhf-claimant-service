package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(Optional.empty(), childrenDatesOfBirth, LocalDate.now());

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

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(Optional.empty(), childrenDatesOfBirth, LocalDate.now());

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

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now());

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

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(Optional.empty(), childrenDatesOfBirth, LocalDate.now());

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

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(Optional.empty(), childrenDatesOfBirth, LocalDate.now());

        // Calculation first three weeks: pregnant = 0 vouchers, children under 1 = 2 vouchers, children under 4 = 1 voucher
        // Calculation last week: pregnant = 0 vouchers, children under 1 = 0 vouchers, children under 4 = 2 vouchers
        // Total = 11 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 3410 pence
        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
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
        LocalDate bornSevenWeeksAgo = LocalDate.now().minusWeeks(7);
        List<LocalDate> childrenDatesOfBirth = singletonList(bornSevenWeeksAgo);

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now(), previousEntitlement);

        // For back dated vouchers: over seven weeks the claimant received seven vouchers for pregnancy.
        // for those seven weeks the claimant had a new child under one which entitles them to 14 vouchers.
        // 14 - 7 = 7
        assertThat(result.getBackdatedVouchers()).isEqualTo(7);
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(4650);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(15);
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
        LocalDate bornTwoWeeksAgo = LocalDate.now().minusWeeks(2);
        LocalDate bornThirteenDaysAgo = LocalDate.now().minusDays(13);
        // date of births representing twins born one day apart
        List<LocalDate> childrenDatesOfBirth = asList(bornTwoWeeksAgo, bornThirteenDaysAgo);

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now(), previousEntitlement);

        // For back dated vouchers: over two weeks the claimant received two vouchers for pregnancy.
        // for week one there was a single child under one, for week two there were two children under one, six vouchers total.
        // 6 - 2 = 4
        assertThat(result.getBackdatedVouchers()).isEqualTo(4);
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(6200);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(20);
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

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now(), previousEntitlement);

        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
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

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now(), previousEntitlement);

        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(2480);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(0);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(4);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(4);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
    }

    // the child was born at the end of the previous cycle, therefore no back dated vouchers are expected.
    @Test
    void shouldCalculateEntitlementWhenThereIsChildBornInTheLastWeekOfThePreviousCycle() {
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().vouchersForPregnancy(1).build();
        PaymentCycleVoucherEntitlement previousEntitlement = new PaymentCycleVoucherEntitlement(singletonList(voucherEntitlement));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().minusWeeks(2));
        LocalDate bornYesterday = LocalDate.now().minusDays(1);
        List<LocalDate> childrenDatesOfBirth = singletonList(bornYesterday);

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now(), previousEntitlement);

        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(2480);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(8);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(0);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0); // we've determine that the child is from the pregnancy, so no longer pregnant
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
    }

    @Test
    void shouldCalculateEntitlementWhenThereHasBeenNoChildNotifiedWithin8WeeksAfterExpectedDueDate() {
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().vouchersForPregnancy(4).build();
        PaymentCycleVoucherEntitlement previousEntitlement = new PaymentCycleVoucherEntitlement(singletonList(voucherEntitlement));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().minusWeeks(8).minusDays(2));
        List<LocalDate> childrenDatesOfBirth = emptyList();

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now(), previousEntitlement);

        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(0);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(0);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(0);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(0);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(0);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
    }

    @Test
    void shouldCalculateEntitlementWhenThereHasBeenNoChildNotifiedAndTheExpectedDueDateGoesOver8WeekThresholdWithinNextPaymentCycle() {
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().vouchersForPregnancy(4).build();
        PaymentCycleVoucherEntitlement previousEntitlement = new PaymentCycleVoucherEntitlement(singletonList(voucherEntitlement));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().minusWeeks(6).minusDays(2));
        List<LocalDate> childrenDatesOfBirth = emptyList();

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now(), previousEntitlement);

        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(620);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(2);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(0);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(0);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(2);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
    }

    @Test
    void shouldCalculateEntitlementWhenThereHasBeenNoChildNotifiedAndTheExpectedDueDateIsExactlyOn8WeekThresholdAtStartOfPaymentCycle() {
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().vouchersForPregnancy(4).build();
        PaymentCycleVoucherEntitlement previousEntitlement = new PaymentCycleVoucherEntitlement(singletonList(voucherEntitlement));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().minusWeeks(8));
        List<LocalDate> childrenDatesOfBirth = emptyList();

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, childrenDatesOfBirth, LocalDate.now(), previousEntitlement);

        assertThat(result.getTotalVoucherValueInPence()).isEqualTo(310);
        assertThat(result.getTotalVoucherEntitlement()).isEqualTo(1);
        assertThat(result.getVouchersForChildrenUnderOne()).isEqualTo(0);
        assertThat(result.getVouchersForChildrenBetweenOneAndFour()).isEqualTo(0);
        assertThat(result.getVouchersForPregnancy()).isEqualTo(1);
        assertThat(result.getVoucherValueInPence()).isEqualTo(310);
        assertThat(result.getBackdatedVouchers()).isEqualTo(0);
    }
}
