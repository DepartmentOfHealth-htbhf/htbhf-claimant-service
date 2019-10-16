package uk.gov.dhsc.htbhf.claimant.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;

@ExtendWith(MockitoExtension.class)
class ClaimantCategoryCalculatorTest {

    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

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
    void shouldMatchClaimantToPregnantWithAge(String age, ClaimantCategory expectedCategory) {
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(2);
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .dateOfBirth(LocalDate.now().minus(Period.parse(age)))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);

        ClaimantCategory claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, emptyList(), LocalDate.now());

        LocalDate now = LocalDate.now();
        assertThat(claimantCategory).isEqualTo(expectedCategory);
        ArgumentCaptor<LocalDate> argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(eq(expectedDeliveryDate), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isAfterOrEqualTo(now);
    }

    @Test
    void shouldMatchClaimantToPregnantWithChildren() {
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(2);
        LocalDate now = LocalDate.now();
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusYears(2));
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);

        ClaimantCategory claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, datesOfBirthOfChildren, now);

        assertThat(claimantCategory).isEqualTo(PREGNANT_WITH_CHILDREN);
        ArgumentCaptor<LocalDate> argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(eq(expectedDeliveryDate), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isAfterOrEqualTo(now);
    }

    @Test
    void shouldMatchClaimantToNotPregnantWithChildren() {
        LocalDate expectedDeliveryDate = null;
        LocalDate now = LocalDate.now();
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusYears(2));
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);

        ClaimantCategory claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, datesOfBirthOfChildren, now);

        assertThat(claimantCategory).isEqualTo(NOT_PREGNANT_WITH_CHILDREN);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(expectedDeliveryDate, now);
    }

    @Test
    void shouldMatchClaimantToNotPregnantWithNoChildren() {
        LocalDate expectedDeliveryDate = null;
        LocalDate now = LocalDate.now();
        List<LocalDate> datesOfBirthOfChildren = emptyList();
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);

        ClaimantCategory claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, datesOfBirthOfChildren, now);

        assertThat(claimantCategory).isEqualTo(NOT_PREGNANT_WITH_NO_CHILDREN);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(expectedDeliveryDate, now);
    }
}
