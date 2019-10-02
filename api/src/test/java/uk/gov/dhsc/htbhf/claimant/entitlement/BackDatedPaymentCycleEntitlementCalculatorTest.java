package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BackDatedPaymentCycleEntitlementCalculatorTest {

    private static final int ENTITLEMENT_CALCULATION_DURATION_IN_DAYS = 2;
    private static final VoucherEntitlement ONE_VOUCHER = VoucherEntitlement.builder().vouchersForChildrenUnderOne(1).build();
    private static final VoucherEntitlement TWO_VOUCHERS = VoucherEntitlement.builder().vouchersForChildrenUnderOne(2).build();

    private EntitlementCalculator entitlementCalculator = mock(EntitlementCalculator.class);
    private PaymentCycleConfig paymentCycleConfig = mock(PaymentCycleConfig.class);

    private BackDatedPaymentCycleEntitlementCalculator backDatedPaymentCycleEntitlementCalculator;

    @BeforeEach
    void init() {
        given(paymentCycleConfig.getEntitlementCalculationDurationInDays()).willReturn(ENTITLEMENT_CALCULATION_DURATION_IN_DAYS);
        backDatedPaymentCycleEntitlementCalculator = new BackDatedPaymentCycleEntitlementCalculator(paymentCycleConfig, entitlementCalculator);
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 6})
    void shouldCalculateBackDatedEntitlementForNewChildGoingBackThreeDurations(Integer numberOfDaysAgoChildWasBorn) {
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now());
        List<LocalDate> newChildrenDatesOfBirth = singletonList(LocalDate.now().minusDays(numberOfDaysAgoChildWasBorn));
        given(entitlementCalculator.calculateVoucherEntitlement(eq(Optional.empty()), anyList(), any())).willReturn(TWO_VOUCHERS);
        given(entitlementCalculator.calculateVoucherEntitlement(any(), eq(emptyList()), any())).willReturn(ONE_VOUCHER);

        int backDatedVouchers = backDatedPaymentCycleEntitlementCalculator.calculateBackDatedVouchers(expectedDueDate,
                newChildrenDatesOfBirth, LocalDate.now());

        // with a cycle duration of two days and a child born six or seven days ago, we must go back three entitlement dates to cover the new child
        // the vouchers for the new child is 2 * 3 = 6
        // the vouchers that would have already been received for pregnancy is 1 * 3 = 3;
        // 6 - 3 = 3
        assertThat(backDatedVouchers).isEqualTo(3);
        // call entitlement calculator with expected due date only to calculate vouchers for pregnancy that have been received.
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, emptyList(), LocalDate.now().minusDays(2));
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, emptyList(), LocalDate.now().minusDays(4));
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, emptyList(), LocalDate.now().minusDays(6));
        // call entitlement calculator with new children date of births only to calculate vouchers for new children
        verify(entitlementCalculator).calculateVoucherEntitlement(Optional.empty(), newChildrenDatesOfBirth, LocalDate.now().minusDays(2));
        verify(entitlementCalculator).calculateVoucherEntitlement(Optional.empty(), newChildrenDatesOfBirth, LocalDate.now().minusDays(4));
        verify(entitlementCalculator).calculateVoucherEntitlement(Optional.empty(), newChildrenDatesOfBirth, LocalDate.now().minusDays(6));
        verifyNoMoreInteractions(entitlementCalculator);
    }

    @Test
    void shouldCalculateBackDatedEntitlementFromDateOfYoungestNewChild() {
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now());
        List<LocalDate> newChildrenDatesOfBirth = asList(LocalDate.now().minusDays(6), LocalDate.now().minusDays(5));
        given(entitlementCalculator.calculateVoucherEntitlement(eq(Optional.empty()), anyList(), any())).willReturn(TWO_VOUCHERS);
        given(entitlementCalculator.calculateVoucherEntitlement(any(), eq(emptyList()), any())).willReturn(ONE_VOUCHER);

        int backDatedVouchers = backDatedPaymentCycleEntitlementCalculator.calculateBackDatedVouchers(expectedDueDate,
                newChildrenDatesOfBirth, LocalDate.now());

        // with a cycle duration of two days and the youngest child born seven days ago, we must go back three entitlement dates to cover the new children
        // the vouchers for the new child is 2 * 3 = 6
        // the vouchers that would have already been received for pregnancy is 1 * 3 = 3;
        // 6 - 3 = 3
        assertThat(backDatedVouchers).isEqualTo(3);
        // call entitlement calculator with expected due date only to calculate vouchers for pregnancy that have been received.
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, emptyList(), LocalDate.now().minusDays(2));
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, emptyList(), LocalDate.now().minusDays(4));
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, emptyList(), LocalDate.now().minusDays(6));
        // call entitlement calculator with new children date of births only to calculate vouchers for new children
        verify(entitlementCalculator).calculateVoucherEntitlement(Optional.empty(), newChildrenDatesOfBirth, LocalDate.now().minusDays(2));
        verify(entitlementCalculator).calculateVoucherEntitlement(Optional.empty(), newChildrenDatesOfBirth, LocalDate.now().minusDays(4));
        verify(entitlementCalculator).calculateVoucherEntitlement(Optional.empty(), newChildrenDatesOfBirth, LocalDate.now().minusDays(6));
        verifyNoMoreInteractions(entitlementCalculator);
    }

    @Test
    void shouldReturnZeroVouchersWhenNumberOfPregnancyVouchersIsHigherThanVouchersForNewChildren() {
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now());
        List<LocalDate> newChildrenDatesOfBirth = singletonList(LocalDate.now().minusDays(3));
        given(entitlementCalculator.calculateVoucherEntitlement(eq(Optional.empty()), anyList(), any())).willReturn(ONE_VOUCHER);
        given(entitlementCalculator.calculateVoucherEntitlement(any(), eq(emptyList()), any())).willReturn(TWO_VOUCHERS);

        int backDatedVouchers = backDatedPaymentCycleEntitlementCalculator.calculateBackDatedVouchers(expectedDueDate,
                newChildrenDatesOfBirth, LocalDate.now());

        // with a cycle duration of two days and a child born three days ago, we must go back one entitlement date to cover the new child
        // the vouchers for the new child is 1 * 1 = 1
        // the vouchers that would have already been received for pregnancy is 2 * 1 = 2;
        // 1 - 2 = -1. Return zero instead of negative number of vouchers
        assertThat(backDatedVouchers).isEqualTo(0);
        // call entitlement calculator with expected due date only to calculate vouchers for pregnancy that have been received.
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, emptyList(), LocalDate.now().minusDays(2));
        // call entitlement calculator with new children date of births only to calculate vouchers for new children
        verify(entitlementCalculator).calculateVoucherEntitlement(Optional.empty(), newChildrenDatesOfBirth, LocalDate.now().minusDays(2));
        verifyNoMoreInteractions(entitlementCalculator);
    }
}
