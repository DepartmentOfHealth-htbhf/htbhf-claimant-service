package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartAndEndDate;

@ExtendWith(MockitoExtension.class)
class AdditionalPregnancyVoucherCalculatorTest {

    private static final int NUMBER_OF_CALCULATION_PERIODS = 4;
    private static final int ENTITLEMENT_CALCULATION_DURATION = 7;
    private static final Integer VOUCHERS_PER_PREGNANCY = 1;

    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    @Mock
    private PaymentCycleConfig paymentCycleConfig;

    private AdditionalPregnancyVoucherCalculator calculator;

    @BeforeEach
    void setUp() {
        given(paymentCycleConfig.getEntitlementCalculationDurationInDays()).willReturn(ENTITLEMENT_CALCULATION_DURATION);
        calculator = new AdditionalPregnancyVoucherCalculator(VOUCHERS_PER_PREGNANCY, paymentCycleConfig, pregnancyEntitlementCalculator);
    }

    @ParameterizedTest
    @CsvSource({
            "-5, 0", // no vouchers if the claim updated date is before the payment cycle start date
            "0, 0",  // if on the same day as the payment cycle then no need for additional vouchers as the payment cycle will now include pregnancy vouchers.
            "1, 3",  // week one, get vouchers for week two, three and four
            "8, 2",  // week two, get vouchers for week three and four
            "15, 1", // week three, get vouchers for week four
            "22, 0"  // week four, get no vouchers
    })
    void shouldCalculateNumberOfAdditionalPregnancyVouchers(int daysInCycle, int expectedNumberOfVouchers) {
        LocalDate expectedDueDate = LocalDate.now().plusMonths(6);
        LocalDate claimUpdatedDate = LocalDate.now();
        LocalDate paymentCycleStartDate = claimUpdatedDate.minusDays(daysInCycle);
        LocalDate paymentCycleEndDate = paymentCycleStartDate.plusDays(NUMBER_OF_CALCULATION_PERIODS * ENTITLEMENT_CALCULATION_DURATION);
        lenient().when(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).thenReturn(true);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(paymentCycleStartDate, paymentCycleEndDate);

        int result = calculator.getAdditionalPregnancyVouchers(expectedDueDate, paymentCycle, claimUpdatedDate);

        assertThat(result).isEqualTo(expectedNumberOfVouchers);
        ArgumentCaptor<LocalDate> argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(pregnancyEntitlementCalculator, times(expectedNumberOfVouchers)).isEntitledToVoucher(eq(expectedDueDate), argumentCaptor.capture());
        List<LocalDate> entitlementDates = argumentCaptor.getAllValues();
        assertThat(entitlementDates).hasSize(expectedNumberOfVouchers);
    }
}
