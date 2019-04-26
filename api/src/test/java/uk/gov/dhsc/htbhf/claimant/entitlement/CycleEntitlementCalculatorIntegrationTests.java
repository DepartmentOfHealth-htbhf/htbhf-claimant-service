package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;

@SpringBootTest
class CycleEntitlementCalculatorIntegrationTests {

    @Autowired
    private CycleEntitlementCalculator cycleEntitlementCalculator;

    @DisplayName("Calculates the correct entitlement when there is a child under one,"
             + " a child under four and neither child changes age during the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForNoChanges() {
        Claimant claimant = aValidClaimantBuilder().build();
        List<LocalDate> childrenDatesOfBirth = asList(LocalDate.now().minusMonths(6), LocalDate.now().minusYears(3));

        int entitlementAmount = cycleEntitlementCalculator.calculateEntitlementInPence(claimant, childrenDatesOfBirth);

        // Calculation per week: pregnant = 0 vouchers, children under 1 = 2 vouchers, children under 4 = 1 voucher
        // Total per week = 3 vouchers. Total over 4 weeks = 12 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 3720 pence
        assertThat(entitlementAmount).isEqualTo(3720);
    }

    @DisplayName("Calculates the correct entitlement for a cycle when there is a child under one, whose first birthday"
             + " is on day two of week one of the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForEntitlementChanges1() {
        Claimant claimant = aValidClaimantBuilder().build();
        LocalDate firstBirthdayOnDayTwoOfWeekOne = LocalDate.now().minusYears(1).plusDays(1);
        List<LocalDate> childrenDatesOfBirth = singletonList(firstBirthdayOnDayTwoOfWeekOne);

        int entitlementAmount = cycleEntitlementCalculator.calculateEntitlementInPence(claimant, childrenDatesOfBirth);

        // Calculation first week: pregnant = 0 voucher, children under 1 = 2 vouchers, children under 4 = 0 vouchers
        // Calculation subsequent weeks: pregnant = 0 voucher, children under 1 = 0 vouchers, children under 4 = 1 voucher
        // Total = 5 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 1550 pence
        assertThat(entitlementAmount).isEqualTo(1550);
    }

    @DisplayName("Calculates the correct entitlement for a cycle when the claimant is pregnant and there is a child between"
             + " one and four whose fourth birthday is on day two of week two of the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForEntitlementChanges2() {
        Claimant pregnantClaimant = aValidClaimantBuilder().expectedDeliveryDate(LocalDate.now().plusMonths(1)).build();
        LocalDate fourthBirthdayOnDayTwoOfWeekTwo = LocalDate.now().minusYears(4).plusWeeks(1).plusDays(1);
        List<LocalDate> childrenDatesOfBirth = singletonList(fourthBirthdayOnDayTwoOfWeekTwo);

        int entitlementAmount = cycleEntitlementCalculator.calculateEntitlementInPence(pregnantClaimant, childrenDatesOfBirth);

        // Calculation first and second week: pregnant = 1 voucher, children under 1 = 0 vouchers, children under 4 = 1 voucher
        // Calculation subsequent weeks: pregnant = 1 voucher, children under 1 = 0 vouchers, children under 4 = 0 vouchers
        // Total = 6 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 1860 pence
        assertThat(entitlementAmount).isEqualTo(1860);
    }

    @DisplayName("Calculates the correct entitlement for a cycle when there is one child under one whose first birthday is on"
             + " day two of week three, and one child between one and four whose fourth birthday is on day one of week four of the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForEntitlementChanges3() {
        Claimant claimant = aValidClaimantBuilder().build();
        LocalDate firstBirthdayOnDayTwoOfWeekThree = LocalDate.now().minusYears(1).plusWeeks(2).plusDays(1);
        LocalDate fourthBirthdayOnDayOneOfWeekFour = LocalDate.now().minusYears(4).plusWeeks(3);
        List<LocalDate> childrenDatesOfBirth = asList(firstBirthdayOnDayTwoOfWeekThree, fourthBirthdayOnDayOneOfWeekFour);

        int entitlementAmount = cycleEntitlementCalculator.calculateEntitlementInPence(claimant, childrenDatesOfBirth);

        // Calculation first three weeks: pregnant = 0 vouchers, children under 1 = 2 vouchers, children under 4 = 1 voucher
        // Calculation last week: pregnant = 0 vouchers, children under 1 = 0 vouchers, children under 4 = 1 vouchers
        // Total = 10 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 3100 pence
        assertThat(entitlementAmount).isEqualTo(3100);
    }

    @DisplayName("Calculates the correct entitlement for a cycle when there is one child under one whose first birthday is on"
             + " day two of week three, and one child between one and four whose fourth birthday is on day two of week four of the payment cycle.")
    @Test
    void shouldCalculateCorrectEntitlementForEntitlementChangesOne4() {
        Claimant claimant = aValidClaimantBuilder().build();
        LocalDate firstBirthdayOnDayTwoOfWeekThree = LocalDate.now().minusYears(1).plusWeeks(2).plusDays(1);
        LocalDate fourthBirthdayOnDayTwoOfWeekFour = LocalDate.now().minusYears(4).plusWeeks(3).plusDays(1);
        List<LocalDate> childrenDatesOfBirth = asList(firstBirthdayOnDayTwoOfWeekThree, fourthBirthdayOnDayTwoOfWeekFour);

        int entitlementAmount = cycleEntitlementCalculator.calculateEntitlementInPence(claimant, childrenDatesOfBirth);

        // Calculation first three weeks: pregnant = 0 vouchers, children under 1 = 2 vouchers, children under 4 = 1 voucher
        // Calculation last week: pregnant = 0 vouchers, children under 1 = 0 vouchers, children under 4 = 2 vouchers
        // Total = 11 vouchers
        // Voucher value = 310 pence. Total voucher value for cycle = 3410 pence
        assertThat(entitlementAmount).isEqualTo(3410);
    }
}