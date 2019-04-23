package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.ChildDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.aValidEligibilityResponseBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.createChildren;
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
        Integer numberOfChildrenUnderOne = 1; // 4 vouchers
        // the inputs include the total number of children under four, NOT the number of children between one and four
        Integer numberOfChildrenUnderFour = 2; // one child aged 1-4 => 3 vouchers
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .vouchersForPregnancy(2)
                .vouchersForChildrenUnderOne(4)
                .vouchersForChildrenBetweenOneAndFour(3)
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();
        LocalDate dueDate = LocalDate.now();
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(dueDate).build();
        List<ChildDTO> children = createChildren(numberOfChildrenUnderOne, numberOfChildrenUnderFour);
        EligibilityResponse response = aValidEligibilityResponseBuilder()
                .children(children)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any())).willReturn(isPregnant);

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, response);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(dueDate);
    }

    @Test
    void shouldReturnZeroVouchersWhenThereAreNoChildren() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build(); // null due date
        EligibilityResponse response = aValidEligibilityResponseBuilder()
                .children(emptyList())
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any())).willReturn(false);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, response);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(null);
    }

    @Test
    void shouldReturnZeroVouchersWhenChildrenAreNull() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build(); // null due date
        EligibilityResponse response = aValidEligibilityResponseBuilder()
                .children(null)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any())).willReturn(false);
        VoucherEntitlement expected = VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .build();

        // When
        VoucherEntitlement result = entitlementCalculator.calculateVoucherEntitlement(claimant, response);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(null);
    }
}
