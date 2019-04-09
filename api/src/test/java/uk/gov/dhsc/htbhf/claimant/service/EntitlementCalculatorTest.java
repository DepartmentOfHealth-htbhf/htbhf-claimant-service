package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.aValidEligibilityResponseBuilder;

@ExtendWith(MockitoExtension.class)
class EntitlementCalculatorTest {

    private static final int VOUCHERS_FOR_CHILDREN_UNDER_ONE = 2;
    private static final int VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR = 1;
    private static final int VOUCHERS_FOR_PREGNANCY = 1;

    @Mock
    PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    EntitlementCalculator entitlementCalculator;


    @BeforeEach
    void setup() {
        entitlementCalculator = new EntitlementCalculator(
                pregnancyEntitlementCalculator,
                VOUCHERS_FOR_CHILDREN_UNDER_ONE,
                VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR,
                VOUCHERS_FOR_PREGNANCY
        );
    }

    @ParameterizedTest(name = "Should return {3} vouchers for pregnant: {0}, number of children under one: {1}, number of children under four: {2}")
    @CsvSource({
            "false, 2, 3, 5",
            "true, 2, 3, 6",
            "true, 0, 0, 1",
            "false, 0, 1, 1",
            "false, 1, 1, 2"
    })
    void shouldReturnCorrectNumberOfVouchers(Boolean isPregnant, Integer numberOfChildrenUnderOne, Integer numberOfChildrenUnderFour, Integer voucherCount) {
        // Given
        LocalDate dueDate = LocalDate.now();
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(dueDate).build();
        EligibilityResponse response = aValidEligibilityResponseBuilder()
                .numberOfChildrenUnderOne(numberOfChildrenUnderOne)
                .numberOfChildrenUnderFour(numberOfChildrenUnderFour)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(dueDate)).willReturn(isPregnant);

        // When
        Integer result = entitlementCalculator.calculateVoucherEntitlement(claimant, response);

        // Then
        assertThat(result).isEqualTo(voucherCount);
    }

    @Test
    void shouldThrowExceptionWhenNumberOfChildrenUnderOneIsGreaterThanNumberOfChildrenUnderFour() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build();
        EligibilityResponse response = aValidEligibilityResponseBuilder()
                .numberOfChildrenUnderOne(6)
                .numberOfChildrenUnderFour(2)
                .build();

        // When
        RuntimeException thrown = catchThrowableOfType(() -> entitlementCalculator.calculateVoucherEntitlement(claimant, response), RuntimeException.class);

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnZeroVouchersWhenNumberOfChildrenIsNull() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build();
        EligibilityResponse response = aValidEligibilityResponseBuilder()
                .numberOfChildrenUnderOne(null)
                .numberOfChildrenUnderFour(null)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(null)).willReturn(false);

        // When
        Integer result = entitlementCalculator.calculateVoucherEntitlement(claimant, response);

        // Then
        assertThat(result).isEqualTo(0);
    }

    @Test
    void shouldReturnCorrectVouchersWhenNumberOfChildrenUnderOneIsNull() {
        // Given
        Claimant claimant = aValidClaimantBuilder().expectedDeliveryDate(null).build();
        EligibilityResponse response = aValidEligibilityResponseBuilder()
                .numberOfChildrenUnderOne(null)
                .numberOfChildrenUnderFour(2)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(null)).willReturn(false);

        // When
        Integer result = entitlementCalculator.calculateVoucherEntitlement(claimant, response);

        // Then
        assertThat(result).isEqualTo(2);
    }

}