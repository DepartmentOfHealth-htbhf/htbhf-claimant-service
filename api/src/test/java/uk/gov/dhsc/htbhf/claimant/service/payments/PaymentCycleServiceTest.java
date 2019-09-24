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
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.FULL_PAYMENT_MADE;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.NEW;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
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

        verifyPaymentCycleSavedCorrectly(claim, result);
        assertThat(result.getPaymentCycleStatus()).isEqualTo(NEW);
        assertThat(result.getCycleStartDate()).isEqualTo(today);
        assertThat(result.getCycleEndDate()).isEqualTo(today.plusDays(PAYMENT_CYCLE_LENGTH - 1));
    }

    @Test
    void shouldCreateNewPaymentCycleWithExpectedDueDateForEligibleClaimWithPregnancyVouchers() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusMonths(9);
        Claim claim = aClaimWithExpectedDeliveryDate(dueDate);
        List<LocalDate> datesOfBirth = List.of(LocalDate.now(), LocalDate.now().minusDays(2));
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycleForEligibleClaim(claim, today, entitlement, datesOfBirth);

        verifyPaymentCycleSavedCorrectly(claim, result);
        assertThat(result.getVoucherEntitlement()).isEqualTo(entitlement);
        assertThat(result.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getPaymentCycleStatus()).isEqualTo(NEW);
        assertThat(result.getCycleStartDate()).isEqualTo(today);
        assertThat(result.getCycleEndDate()).isEqualTo(today.plusDays(PAYMENT_CYCLE_LENGTH - 1));
        assertThat(result.getChildrenDob()).isEqualTo(datesOfBirth);
        assertThat(result.getExpectedDeliveryDate()).isEqualTo(dueDate);
        assertThat(result.getTotalEntitlementAmountInPence()).isEqualTo(entitlement.getTotalVoucherValueInPence());
        assertThat(result.getTotalVouchers()).isEqualTo(entitlement.getTotalVoucherEntitlement());
    }

    @Test
    void shouldCreateNewPaymentCycleWithoutExpectedDueDateForEligibleClaimWithoutPregnancyVouchers() {
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(9);
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);
        List<LocalDate> datesOfBirth = List.of(LocalDate.now(), LocalDate.now().minusDays(2));
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers();

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycleForEligibleClaim(claim, expectedDeliveryDate, entitlement, datesOfBirth);

        verifyPaymentCycleSavedCorrectly(claim, result);
        assertThat(result.getVoucherEntitlement()).isEqualTo(entitlement);
        assertThat(result.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getPaymentCycleStatus()).isEqualTo(NEW);
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

    @Test
    void shouldGetExpectedDeliveryDateIfPregnancyVouchersExist() {
        LocalDate expectedDeliveryDate = LocalDate.now();
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();

        LocalDate result = paymentCycleService.getExpectedDeliveryDateIfRelevant(claim, voucherEntitlement);

        assertThat(result).isEqualTo(expectedDeliveryDate);
    }

    @Test
    void shouldReturnNullForExpectedDeliveryDateIfPregnancyVouchersDoNotExist() {
        LocalDate expectedDeliveryDate = LocalDate.now().minusMonths(6);
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers();

        LocalDate result = paymentCycleService.getExpectedDeliveryDateIfRelevant(claim, voucherEntitlement);

        assertThat(result).isNull();
    }

    @Test
    void shouldUpdatePaymentCycleWithStatusAndCardBalance() {
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .paymentCycleStatus(NEW)
                .cardBalanceInPence(null)
                .build();
        int newCardBalance = 200;
        LocalDateTime now = LocalDateTime.now();

        paymentCycleService.updatePaymentCycle(paymentCycle, FULL_PAYMENT_MADE, newCardBalance);

        assertThat(paymentCycle.getCardBalanceTimestamp()).isAfterOrEqualTo(now);
        assertThat(paymentCycle.getCardBalanceInPence()).isEqualTo(newCardBalance);
        assertThat(paymentCycle.getPaymentCycleStatus()).isEqualTo(FULL_PAYMENT_MADE);
    }

    @Test
    void shouldUpdatePaymentCycleWithEligibilityAndEntitlementDecision() {
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(2);
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .claim(claim)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .paymentCycleStatus(NEW)
                .childrenDob(null)
                .voucherEntitlement(null)
                .expectedDeliveryDate(null)
                .build();
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementDecision.builder()
                .dateOfBirthOfChildren(List.of(LocalDate.now().minusYears(1)))
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .voucherEntitlement(voucherEntitlement)
                .build();

        paymentCycleService.updatePaymentCycle(paymentCycle, decision);

        assertThat(paymentCycle.getId()).isEqualTo(paymentCycle.getId());
        assertThat(paymentCycle.getEligibilityStatus()).isEqualTo(decision.getEligibilityStatus());
        assertThat(paymentCycle.getChildrenDob()).isEqualTo(decision.getDateOfBirthOfChildren());
        assertThat(paymentCycle.getTotalEntitlementAmountInPence()).isEqualTo(decision.getVoucherEntitlement().getTotalVoucherValueInPence());
        assertThat(paymentCycle.getTotalVouchers()).isEqualTo(decision.getVoucherEntitlement().getTotalVoucherEntitlement());
        assertThat(paymentCycle.getExpectedDeliveryDate()).isEqualTo(expectedDeliveryDate);
    }

    private void verifyPaymentCycleSavedCorrectly(Claim claim, PaymentCycle result) {
        ArgumentCaptor<PaymentCycle> argumentCaptor = ArgumentCaptor.forClass(PaymentCycle.class);
        verify(paymentCycleRepository).save(argumentCaptor.capture());
        PaymentCycle paymentCycle = argumentCaptor.getValue();
        assertThat(paymentCycle.getClaim()).isEqualTo(claim);
        assertThat(result).isEqualTo(paymentCycle);
    }
}
