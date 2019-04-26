package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHER_VALUE_IN_PENCE;

@ExtendWith(MockitoExtension.class)
class EntitlementCalculatorTest {

    private static final int VOUCHERS_FOR_CHILDREN_UNDER_ONE = 4;
    private static final int VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR = 3;
    private static final int VOUCHERS_FOR_PREGNANCY = 2;

    @Mock
    PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    private EntitlementCalculator entitlementCalculator;


    @BeforeEach
    void setup() {
        entitlementCalculator = new EntitlementCalculator(
                pregnancyEntitlementCalculator,
                VOUCHERS_FOR_CHILDREN_UNDER_ONE,
                VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR,
                VOUCHERS_FOR_PREGNANCY,
                VOUCHER_VALUE_IN_PENCE
        );
    }

    @Test
    void shouldReturnCorrectEntitlement() {
        // Given
        Boolean isPregnant = true; // 2 vouchers
        // one child under one, one between one and four.
        List<LocalDate> childrenDateOfBirths = asList(LocalDate.now().minusMonths(6), LocalDate.now().minusYears(3));
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .vouchersForPregnancy(2)
                .vouchersForChildrenUnderOne(4)
                .vouchersForChildrenBetweenOneAndFour(3)
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();
        LocalDate dueDate = LocalDate.now();
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(dueDate).build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any())).willReturn(isPregnant);

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, childrenDateOfBirths);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(dueDate);
    }

    @Test
    void shouldReturnZeroVouchersWhenThereAreNoChildren() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build(); // null due date
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any())).willReturn(false);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, emptyList());

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(null);
    }

    @Test
    void shouldReturnZeroVouchersWhenChildrenAreNull() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build(); // null due date
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any())).willReturn(false);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, null);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(null);
    }
}
