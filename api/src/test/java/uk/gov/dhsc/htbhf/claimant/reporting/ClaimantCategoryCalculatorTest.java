package uk.gov.dhsc.htbhf.claimant.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.TestConstants.TWENTY_YEAR_OLD;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.NOT_PREGNANT_WITH_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.NOT_PREGNANT_WITH_NO_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.PREGNANT_WITH_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NOT_PREGNANT;

@ExtendWith(MockitoExtension.class)
class ClaimantCategoryCalculatorTest {

    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;

    @InjectMocks
    private ClaimantCategoryCalculator claimantCategoryCalculator;

    @ParameterizedTest(name = "Pregnant claimant aged {0} with no children is {1}")
    @CsvSource({
            "P14Y, PREGNANT_AND_UNDER_16",
            "P15Y, PREGNANT_AND_UNDER_16",
            "P15Y11M27D, PREGNANT_AND_UNDER_16",
            "P16Y, PREGNANT_AND_UNDER_18",
            "P17Y, PREGNANT_AND_UNDER_18",
            "P17Y11M27D, PREGNANT_AND_UNDER_18",
            "P18Y, PREGNANT_WITH_NO_CHILDREN",
            "P19Y, PREGNANT_WITH_NO_CHILDREN",
            "P19Y, PREGNANT_WITH_NO_CHILDREN",
            "P20Y, PREGNANT_WITH_NO_CHILDREN",
            "P40Y, PREGNANT_WITH_NO_CHILDREN",
    })
    void shouldMatchClaimantToCategoryAtAge(String age, ClaimantCategory expectedCategory) {
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS)
                .dateOfBirth(LocalDate.now().minus(Period.parse(age)))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);
        LocalDate atDate = LocalDate.now();

        ClaimantCategory claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, NO_CHILDREN, atDate);

        assertThat(claimantCategory).isEqualTo(expectedCategory);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, atDate);
    }

    @Test
    void shouldMatchClaimantToPregnantWithChildren() {
        LocalDate atDate = LocalDate.now();
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusYears(2));
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS)
                .dateOfBirth(TWENTY_YEAR_OLD)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);
        given(childDateOfBirthCalculator.hasChildrenUnderFourAtGivenDate(any(), any())).willReturn(true);

        ClaimantCategory claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, datesOfBirthOfChildren, atDate);

        assertThat(claimantCategory).isEqualTo(PREGNANT_WITH_CHILDREN);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, atDate);
        verify(childDateOfBirthCalculator).hasChildrenUnderFourAtGivenDate(datesOfBirthOfChildren, atDate);
    }

    @Test
    void shouldMatchClaimantToNotPregnantWithChildren() {
        LocalDate atDate = LocalDate.now();
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusYears(2));
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(NOT_PREGNANT)
                .dateOfBirth(TWENTY_YEAR_OLD)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);
        given(childDateOfBirthCalculator.hasChildrenUnderFourAtGivenDate(any(), any())).willReturn(true);

        ClaimantCategory claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, datesOfBirthOfChildren, atDate);

        assertThat(claimantCategory).isEqualTo(NOT_PREGNANT_WITH_CHILDREN);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(NOT_PREGNANT, atDate);
        verify(childDateOfBirthCalculator).hasChildrenUnderFourAtGivenDate(datesOfBirthOfChildren, atDate);
    }

    @Test
    void shouldMatchClaimantToNotPregnantWithNoChildren() {
        LocalDate atDate = LocalDate.now();
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(NOT_PREGNANT)
                .dateOfBirth(TWENTY_YEAR_OLD)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);
        given(childDateOfBirthCalculator.hasChildrenUnderFourAtGivenDate(any(), any())).willReturn(false);

        ClaimantCategory claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, NO_CHILDREN, atDate);

        assertThat(claimantCategory).isEqualTo(NOT_PREGNANT_WITH_NO_CHILDREN);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(NOT_PREGNANT, atDate);
        verify(childDateOfBirthCalculator).hasChildrenUnderFourAtGivenDate(NO_CHILDREN, atDate);
    }
}
