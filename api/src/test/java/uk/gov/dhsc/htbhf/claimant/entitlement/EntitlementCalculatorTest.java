package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
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
        LocalDate entitlementDate = LocalDate.now();
        Boolean isPregnant = true; // 2 vouchers
        // three children under one, two between one and four.
        List<LocalDate> childrenDateOfBirths = createChildrenDateOfBirths(3, 2, entitlementDate);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .vouchersForPregnancy(2)
                .vouchersForChildrenUnderOne(12)
                .vouchersForChildrenBetweenOneAndFour(6)
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();
        LocalDate dueDate = LocalDate.now();
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(dueDate).build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(isPregnant);

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, childrenDateOfBirths, entitlementDate);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(dueDate, LocalDate.now());
    }

    @Test
    void shouldReturnZeroVouchersWhenThereAreNoChildren() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build(); // null due date
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, emptyList(), LocalDate.now());

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(null, LocalDate.now());
    }

    @Test
    void shouldReturnZeroVouchersWhenChildrenAreNull() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build(); // null due date
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, null, LocalDate.now());

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(null, LocalDate.now());
    }

    // TODO better name
    private List<LocalDate> createChildrenDateOfBirths(int numberOfChildrenUnderOne, int numberOfChildrenBetweenOneAndFour, LocalDate entitlementDate) {
        List<LocalDate> childrenUnderOne = nCopies(numberOfChildrenUnderOne, entitlementDate.minusMonths(6));
        List<LocalDate> childrenBetweenOneAndFour = nCopies(numberOfChildrenBetweenOneAndFour, entitlementDate.minusYears(3));

        return Stream.concat(childrenUnderOne.stream(), childrenBetweenOneAndFour.stream()).collect(Collectors.toList());
    }
}
