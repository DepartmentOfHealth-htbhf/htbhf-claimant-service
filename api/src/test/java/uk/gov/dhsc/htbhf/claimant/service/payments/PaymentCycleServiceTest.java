package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers;

@ExtendWith(MockitoExtension.class)
class PaymentCycleServiceTest {

    private static final int PAYMENT_CYCLE_LENGTH = 28;

    @Mock
    private PaymentCycleRepository paymentCycleRepository;

    private PaymentCycleService paymentCycleService;

    @BeforeEach
    void setup() {
        paymentCycleService = new PaymentCycleService(paymentCycleRepository, PAYMENT_CYCLE_LENGTH);
    }

    @Test
    void shouldCreateNewPaymentCycle() {
        LocalDate today = LocalDate.now();
        Claim claim = aValidClaim();

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycle(claim, today);

        verifyPaymentCycleSavedCorrectly(today, claim, result);
    }

    @Test
    void shouldCreateNewPaymentCycleWithExpectedDueDateForEligibleClaimWithPregnancyVouchers() {
        LocalDate today = LocalDate.now();
        Claim claim = aClaimWithExpectedDeliveryDate(today);
        List<LocalDate> datesOfBirth = List.of(LocalDate.now(), LocalDate.now().minusDays(2));
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycleForEligibleClaim(claim, today, entitlement, datesOfBirth);

        verifyPaymentCycleSavedCorrectly(today, claim, result);
        assertThat(result.getVoucherEntitlement()).isEqualTo(entitlement);
        assertThat(result.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.NEW);
        assertThat(result.getChildrenDob()).isEqualTo(datesOfBirth);
        assertThat(result.getExpectedDeliveryDate()).isEqualTo(today);
        assertThat(result.getTotalEntitlementAmountInPence()).isEqualTo(entitlement.getTotalVoucherValueInPence());
        assertThat(result.getTotalVouchers()).isEqualTo(entitlement.getTotalVoucherEntitlement());
    }

    @Test
    void shouldCreateNewPaymentCycleWithoutExpectedDueDateForEligibleClaimWithoutPregnancyVouchers() {
        LocalDate today = LocalDate.now();
        Claim claim = aClaimWithExpectedDeliveryDate(today);
        List<LocalDate> datesOfBirth = List.of(LocalDate.now(), LocalDate.now().minusDays(2));
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers();

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycleForEligibleClaim(claim, today, entitlement, datesOfBirth);

        verifyPaymentCycleSavedCorrectly(today, claim, result);
        assertThat(result.getVoucherEntitlement()).isEqualTo(entitlement);
        assertThat(result.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.NEW);
        assertThat(result.getChildrenDob()).isEqualTo(datesOfBirth);
        assertThat(result.getExpectedDeliveryDate()).isNull();
        assertThat(result.getTotalEntitlementAmountInPence()).isEqualTo(entitlement.getTotalVoucherValueInPence());
        assertThat(result.getTotalVouchers()).isEqualTo(entitlement.getTotalVoucherEntitlement());
    }

    @Test
    void shouldSavePaymentCycle() {
        PaymentCycle paymentCycle = aValidPaymentCycle();

        paymentCycleService.savePaymentCycle(paymentCycle);

        verify(paymentCycleRepository).save(paymentCycle);
    }

    private void verifyPaymentCycleSavedCorrectly(LocalDate today, Claim claim, PaymentCycle result) {
        ArgumentCaptor<PaymentCycle> argumentCaptor = ArgumentCaptor.forClass(PaymentCycle.class);
        verify(paymentCycleRepository).save(argumentCaptor.capture());
        PaymentCycle paymentCycle = argumentCaptor.getValue();
        assertThat(paymentCycle.getClaim()).isEqualTo(claim);
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(today);
        assertThat(paymentCycle.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.NEW);
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(today.plusDays(PAYMENT_CYCLE_LENGTH));
        assertThat(result).isEqualTo(paymentCycle);
    }
}
