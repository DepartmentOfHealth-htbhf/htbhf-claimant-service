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
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;

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
    void shouldCreateNewPaymentCycleForEligibleClaim() {
        LocalDate today = LocalDate.now();
        Claim claim = aValidClaim();
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycleForEligibleClaim(claim, today, entitlement);

        verifyPaymentCycleSavedCorrectly(today, claim, result);
        assertThat(result.getVoucherEntitlement()).isEqualTo(entitlement);
        assertThat(result.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
    }

    private void verifyPaymentCycleSavedCorrectly(LocalDate today, Claim claim, PaymentCycle result) {
        ArgumentCaptor<PaymentCycle> argumentCaptor = ArgumentCaptor.forClass(PaymentCycle.class);
        verify(paymentCycleRepository).save(argumentCaptor.capture());
        PaymentCycle paymentCycle = argumentCaptor.getValue();
        assertThat(paymentCycle.getClaim()).isEqualTo(claim);
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(today);
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(today.plusDays(PAYMENT_CYCLE_LENGTH));
        assertThat(result).isEqualTo(paymentCycle);
    }
}
