package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
        LocalDate entitlementDate = LocalDate.now();
        Boolean isPregnant = true; // 2 vouchers
        // three children under one, two between one and four.
        List<LocalDate> dateOfBirthOfChildren = createDateOfBirthForChildren(3, 2, entitlementDate);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .vouchersForPregnancy(2)
                .vouchersForChildrenUnderOne(12)
                .vouchersForChildrenBetweenOneAndFour(6)
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .entitlementDate(entitlementDate)
                .build();
        LocalDate dueDate = LocalDate.now();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(isPregnant);

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(Optional.of(dueDate), dateOfBirthOfChildren, entitlementDate);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(dueDate, LocalDate.now());
    }

    @Test
    void shouldReturnZeroVouchersWhenThereAreNoChildrenAndNotPregnant() {
        // Given
        LocalDate entitlementDate = LocalDate.now();
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .entitlementDate(entitlementDate)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(Optional.empty(), emptyList(), entitlementDate);

        // Then
        assertThat(result).isEqualTo(expected);
        verifyZeroInteractions(pregnancyEntitlementCalculator);
    }

    @Test
    void shouldReturnZeroVouchersWhenChildrenAreNullAndNotPregnant() {
        // Given
        LocalDate entitlementDate = LocalDate.now();
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .entitlementDate(entitlementDate)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(Optional.empty(), null, entitlementDate);

        // Then
        assertThat(result).isEqualTo(expected);
        verifyZeroInteractions(pregnancyEntitlementCalculator);
    }

    @Test
    void shouldReturnZeroVouchersForChildBornAfterEntitlementDate() {
        // Given
        LocalDate entitlementDate = LocalDate.now().minusDays(2);
        LocalDate dateOfBirth = entitlementDate.plusDays(1);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .entitlementDate(entitlementDate)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(Optional.empty(), singletonList(dateOfBirth), entitlementDate);

        // Then
        assertThat(result).isEqualTo(expected);
        verifyZeroInteractions(pregnancyEntitlementCalculator);
    }

    @Test
    void shouldReturnCorrectVouchersForChildBornOnEntitlementDate() {
        // Given
        LocalDate entitlementDate = LocalDate.now().minusDays(2);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .vouchersForChildrenUnderOne(VOUCHERS_FOR_CHILDREN_UNDER_ONE)
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .entitlementDate(entitlementDate)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(Optional.empty(), singletonList(entitlementDate), entitlementDate);

        // Then
        assertThat(result).isEqualTo(expected);
        verifyZeroInteractions(pregnancyEntitlementCalculator);
    }

    private List<LocalDate> createDateOfBirthForChildren(int numberOfChildrenUnderOne, int numberOfChildrenBetweenOneAndFour, LocalDate entitlementDate) {
        List<LocalDate> childrenUnderOne = nCopies(numberOfChildrenUnderOne, entitlementDate.minusMonths(6));
        List<LocalDate> childrenBetweenOneAndFour = nCopies(numberOfChildrenBetweenOneAndFour, entitlementDate.minusYears(3));

        return Stream.concat(childrenUnderOne.stream(), childrenBetweenOneAndFour.stream()).collect(Collectors.toList());
    }
}
