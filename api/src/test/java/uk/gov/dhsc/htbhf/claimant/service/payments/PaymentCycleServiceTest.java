package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory;
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.FULL_PAYMENT_MADE;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.NEW;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_TOO_FAR_IN_PAST;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NOT_PREGNANT;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class PaymentCycleServiceTest {

    private static final int PAYMENT_CYCLE_LENGTH = 28;

    @Mock
    private PaymentCycleRepository paymentCycleRepository;
    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    private PaymentCycleService paymentCycleService;

    @BeforeEach
    void setup() {
        paymentCycleService = new PaymentCycleService(paymentCycleRepository, PAYMENT_CYCLE_LENGTH, pregnancyEntitlementCalculator);
    }

    @Test
    void shouldCreateNewPaymentCycle() {
        LocalDate today = LocalDate.now();
        Claim claim = aValidClaim();

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycle(claim, today);

        verifyPaymentCycleSavedCorrectly(claim, result);
        assertThat(result.getPaymentCycleStatus()).isEqualTo(NEW);
        assertThat(result.getEligibilityStatus()).isNull();
        assertThat(result.getIdentityAndEligibilityResponse()).isNull();
        assertThat(result.getCycleStartDate()).isEqualTo(today);
        assertThat(result.getCycleEndDate()).isEqualTo(today.plusDays(PAYMENT_CYCLE_LENGTH - 1));
        verifyNoInteractions(pregnancyEntitlementCalculator);
    }

    @Test
    void shouldCreateNewPaymentCycleWithExpectedDueDateForEligibleClaimWithPregnancyVouchers() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusMonths(9);
        Claim claim = aClaimWithExpectedDeliveryDate(dueDate);
        List<LocalDate> datesOfBirth = List.of(LocalDate.now(), LocalDate.now().minusDays(2));
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();
        IdentityAndEligibilityResponse identityAndEligibilityResponse = anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(datesOfBirth);
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementTestDataFactory.aValidDecisionBuilder()
                .existingClaimId(claim.getId())
                .voucherEntitlement(entitlement)
                .dateOfBirthOfChildren(datesOfBirth)
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycleForEligibleClaim(claim, today, decision);

        verifyPaymentCycleSavedCorrectly(claim, result);
        assertThat(result.getVoucherEntitlement()).isEqualTo(entitlement);
        assertThat(result.getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(result.getIdentityAndEligibilityResponse()).isEqualTo(identityAndEligibilityResponse);
        assertThat(result.getPaymentCycleStatus()).isEqualTo(NEW);
        assertThat(result.getCycleStartDate()).isEqualTo(today);
        assertThat(result.getCycleEndDate()).isEqualTo(today.plusDays(PAYMENT_CYCLE_LENGTH - 1));
        assertThat(result.getChildrenDob()).isEqualTo(datesOfBirth);
        assertThat(result.getExpectedDeliveryDate()).isEqualTo(dueDate);
        assertThat(result.getTotalEntitlementAmountInPence()).isEqualTo(entitlement.getTotalVoucherValueInPence());
        assertThat(result.getTotalVouchers()).isEqualTo(entitlement.getTotalVoucherEntitlement());
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(dueDate, result.getCycleStartDate());
    }

    @Test
    void shouldCreateNewPaymentCycleWithoutExpectedDueDateForEligibleClaimWithoutPregnancyVouchers() {
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(9);
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);
        List<LocalDate> datesOfBirth = List.of(LocalDate.now(), LocalDate.now().minusDays(2));
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers();
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementTestDataFactory.aValidDecisionBuilder()
                .existingClaimId(claim.getId())
                .voucherEntitlement(entitlement)
                .dateOfBirthOfChildren(datesOfBirth)
                .identityAndEligibilityResponse(anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(datesOfBirth))
                .build();
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycleForEligibleClaim(claim, expectedDeliveryDate, decision);

        verifyPaymentCycleSavedCorrectly(claim, result);
        assertThat(result.getVoucherEntitlement()).isEqualTo(entitlement);
        assertThat(result.getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(result.getIdentityAndEligibilityResponse()).isEqualTo(anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(datesOfBirth));
        assertThat(result.getPaymentCycleStatus()).isEqualTo(NEW);
        assertThat(result.getChildrenDob()).isEqualTo(datesOfBirth);
        assertThat(result.getExpectedDeliveryDate()).isNull();
        assertThat(result.getTotalEntitlementAmountInPence()).isEqualTo(entitlement.getTotalVoucherValueInPence());
        assertThat(result.getTotalVouchers()).isEqualTo(entitlement.getTotalVoucherEntitlement());
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(expectedDeliveryDate, result.getCycleStartDate());
    }

    @Test
    void shouldSavePaymentCycle() {
        PaymentCycle paymentCycle = aValidPaymentCycle();

        paymentCycleService.savePaymentCycle(paymentCycle);

        verify(paymentCycleRepository).save(paymentCycle);
        verifyNoInteractions(pregnancyEntitlementCalculator);
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
        verifyNoInteractions(pregnancyEntitlementCalculator);
    }

    @Test
    void shouldUpdatePaymentCycleWithEligibilityAndEntitlementDecisionWithNoExpectedDeliveryDate() {

        PaymentCycle paymentCycle = buildPaymentCycle(EXPECTED_DELIVERY_DATE_TOO_FAR_IN_PAST);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();
        EligibilityAndEntitlementDecision decision = buildEligibilityAndEntitlementDecision(voucherEntitlement);
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);

        paymentCycleService.updatePaymentCycle(paymentCycle, decision);

        verifyPaymentCycleUpdatedCorrectly(NOT_PREGNANT, paymentCycle, decision);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(EXPECTED_DELIVERY_DATE_TOO_FAR_IN_PAST, paymentCycle.getCycleStartDate());
    }

    @Test
    void shouldUpdatePaymentCycleWithEligibilityAndEntitlementDecisionAndExpectedDeliveryDate() {
        PaymentCycle paymentCycle = buildPaymentCycle(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();
        EligibilityAndEntitlementDecision decision = buildEligibilityAndEntitlementDecision(voucherEntitlement);
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);

        paymentCycleService.updatePaymentCycle(paymentCycle, decision);

        verifyPaymentCycleUpdatedCorrectly(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, paymentCycle, decision);
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, paymentCycle.getCycleStartDate());
    }

    private PaymentCycle buildPaymentCycle(LocalDate expectedDeliveryDate) {
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);
        return aValidPaymentCycleBuilder()
                .claim(claim)
                .eligibilityStatus(ELIGIBLE)
                .paymentCycleStatus(NEW)
                .childrenDob(null)
                .voucherEntitlement(null)
                .expectedDeliveryDate(null)
                .build();
    }

    private EligibilityAndEntitlementDecision buildEligibilityAndEntitlementDecision(PaymentCycleVoucherEntitlement voucherEntitlement) {
        List<LocalDate> dateOfBirthOfChildren = List.of(LocalDate.now().minusYears(1));
        return EligibilityAndEntitlementDecision.builder()
                .dateOfBirthOfChildren(dateOfBirthOfChildren)
                .eligibilityStatus(INELIGIBLE)
                .identityAndEligibilityResponse(anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(dateOfBirthOfChildren))
                .voucherEntitlement(voucherEntitlement)
                .build();
    }

    private void verifyPaymentCycleUpdatedCorrectly(LocalDate expectedDeliveryDate, PaymentCycle paymentCycle, EligibilityAndEntitlementDecision decision) {
        assertThat(paymentCycle.getId()).isEqualTo(paymentCycle.getId());
        assertThat(paymentCycle.getEligibilityStatus()).isEqualTo(INELIGIBLE);
        assertThat(paymentCycle.getIdentityAndEligibilityResponse()).isEqualTo(decision.getIdentityAndEligibilityResponse());
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
