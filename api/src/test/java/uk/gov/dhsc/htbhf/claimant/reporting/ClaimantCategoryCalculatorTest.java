package uk.gov.dhsc.htbhf.claimant.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;

@ExtendWith(MockitoExtension.class)
class ClaimantCategoryCalculatorTest {

    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    @InjectMocks
    private ClaimantCategoryCalculator claimantCategoryCalculator;

    @Test
    void shouldMatchClaimantToPregnantAndUnderSixteen() {
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(2);
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .dateOfBirth(LocalDate.now().minusYears(15))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);

        String claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, emptyList(), LocalDateTime.now());

        LocalDate now = LocalDate.now();
        assertThat(claimantCategory).isEqualTo("Pregnant and under 16");
        ArgumentCaptor<LocalDate> argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(eq(expectedDeliveryDate), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isAfterOrEqualTo(now);
    }

    @Test
    void shouldMatchClaimantToPregnantWithChildren() {
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(2);
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusYears(2));
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);

        String claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, datesOfBirthOfChildren, LocalDateTime.now());

        LocalDate now = LocalDate.now();
        assertThat(claimantCategory).isEqualTo("Pregnant with passported children");
        ArgumentCaptor<LocalDate> argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(eq(expectedDeliveryDate), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isAfterOrEqualTo(now);
    }

    @Test
    void shouldMatchClaimantToPregnantWithNoChildren() {
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(2);
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);

        String claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, emptyList(), LocalDateTime.now());

        LocalDate now = LocalDate.now();
        assertThat(claimantCategory).isEqualTo("Pregnant and no passported children");
        ArgumentCaptor<LocalDate> argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(pregnancyEntitlementCalculator, times(2)).isEntitledToVoucher(eq(expectedDeliveryDate), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isAfterOrEqualTo(now);
    }

    @Test
    void shouldMatchClaimantToNotPregnantWithChildren() {
        LocalDate expectedDeliveryDate = null;
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusYears(2));
        Claimant claimant = aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);

        String claimantCategory = claimantCategoryCalculator.determineClaimantCategory(claimant, datesOfBirthOfChildren, LocalDateTime.now());

        LocalDate now = LocalDate.now();
        assertThat(claimantCategory).isEqualTo("Not pregnant with passported children");
        ArgumentCaptor<LocalDate> argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(pregnancyEntitlementCalculator, times(3)).isEntitledToVoucher(eq(expectedDeliveryDate), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isAfterOrEqualTo(now);
    }
}
