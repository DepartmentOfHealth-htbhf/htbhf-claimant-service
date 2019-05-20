package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.CycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithZeroVouchers;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class EligibilityAndEntitlementServiceTest {

    @InjectMocks
    EligibilityAndEntitlementService eligibilityAndEntitlementService;

    @Mock
    EligibilityClient client;

    @Mock
    EligibilityStatusCalculator eligibilityStatusCalculator;

    @Mock
    ClaimRepository claimRepository;

    @Mock
    CycleEntitlementCalculator cycleEntitlementCalculator;

    @Test
    void shouldReturnDuplicateResponseWhenLiveClaimAlreadyExists() {
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(true);
        Claimant claimant = aValidClaimant();

        EligibilityAndEntitlementDecision eligibilityResponse = eligibilityAndEntitlementService.evaluateNewClaimant(claimant);

        assertThat(eligibilityResponse.getEligibilityStatus()).isEqualTo(DUPLICATE);
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verifyZeroInteractions(client, eligibilityStatusCalculator, cycleEntitlementCalculator);
    }

    @Test
    void shouldReturnEligibleWhenNotDuplicateAndEligibleWithVouchers() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatusForNewClaim(any())).willReturn(ELIGIBLE);
        given(cycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateNewClaimant(claimant);

        assertCorrectResult(result, ELIGIBLE, eligibilityResponse, voucherEntitlement);
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(eligibilityStatusCalculator).determineEligibilityStatusForNewClaim(eligibilityResponse);
        verify(cycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                LocalDate.now());
    }

    @Test
    void shouldReturnIneligibleWhenNotDuplicateAndEligibleWithoutVouchers() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatusForNewClaim(any())).willReturn(ELIGIBLE);
        given(cycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateNewClaimant(claimant);

        assertCorrectResult(result, INELIGIBLE, eligibilityResponse, voucherEntitlement);
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(eligibilityStatusCalculator).determineEligibilityStatusForNewClaim(eligibilityResponse);
        verify(cycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                LocalDate.now());
    }

    @Test
    void shouldReturnEligibleForExistingClaimantWithVouchers() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        LocalDate cycleStartDate = LocalDate.now().minusDays(1);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        PaymentCycle previousCycle = PaymentCycleTestDataFactory.aValidPaymentCycle();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(cycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result =
                eligibilityAndEntitlementService.evaluateExistingClaimant(claimant, cycleStartDate, previousCycle);

        assertCorrectResult(result, ELIGIBLE, eligibilityResponse, voucherEntitlement);
        verify(client).checkEligibility(claimant);
        verify(cycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        verifyZeroInteractions(eligibilityStatusCalculator, claimRepository);
    }

    @Test
    void shouldReturnIneligibleForExistingClaimantWithoutVouchers() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        LocalDate cycleStartDate = LocalDate.now().minusDays(1);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();
        PaymentCycle previousCycle = PaymentCycleTestDataFactory.aValidPaymentCycle();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(cycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result =
                eligibilityAndEntitlementService.evaluateExistingClaimant(claimant, cycleStartDate, previousCycle);

        assertCorrectResult(result, INELIGIBLE, eligibilityResponse, voucherEntitlement);
        verify(client).checkEligibility(claimant);
        verify(cycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        verifyZeroInteractions(eligibilityStatusCalculator, claimRepository);
    }

    private void assertCorrectResult(EligibilityAndEntitlementDecision result,
                                     EligibilityStatus eligibilityStatus,
                                     EligibilityResponse eligibilityResponse,
                                     PaymentCycleVoucherEntitlement voucherEntitlement) {
        assertThat(result).isNotNull();
        assertThat(result.getEligibilityStatus()).isEqualTo(eligibilityStatus);
        assertThat(result.getVoucherEntitlement()).isEqualTo(voucherEntitlement);
        assertThat(result.getDateOfBirthOfChildren()).isEqualTo(eligibilityResponse.getDateOfBirthOfChildren());
        assertThat(result.getDwpHouseholdIdentifier()).isEqualTo(eligibilityResponse.getDwpHouseholdIdentifier());
        assertThat(result.getHmrcHouseholdIdentifier()).isEqualTo(eligibilityResponse.getHmrcHouseholdIdentifier());
    }

}
