package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.exception.MultipleClaimsWithSameNinoException;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithZeroVouchers;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class EligibilityAndEntitlementServiceTest {

    @InjectMocks
    EligibilityAndEntitlementService eligibilityAndEntitlementService;

    @Mock
    EligibilityClient client;

    @Mock
    DuplicateClaimChecker duplicateClaimChecker;

    @Mock
    ClaimRepository claimRepository;

    @Mock
    PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;

    @Test
    void shouldReturnExistingClaimIdWhenLiveClaimAlreadyExists() {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        UUID existingClaimId = UUID.randomUUID();
        given(claimRepository.findLiveClaimsWithNino(any())).willReturn(List.of(existingClaimId));
        Claimant claimant = aValidClaimant();

        EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateClaimant(claimant);

        assertThat(decision.getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(decision.getExistingClaimId()).isEqualTo(existingClaimId);
        assertThat(decision.getVoucherEntitlement()).isEqualTo(voucherEntitlement);
        verify(claimRepository).findLiveClaimsWithNino(claimant.getNino());
        verifyZeroInteractions(client, duplicateClaimChecker);
    }

    @Test
    void shouldThrowExceptionWhenMultipleLiveClaimsExistForNino() {
        List<UUID> existingIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        given(claimRepository.findLiveClaimsWithNino(any())).willReturn(existingIds);
        Claimant claimant = aValidClaimant();

        MultipleClaimsWithSameNinoException exception = catchThrowableOfType(() -> eligibilityAndEntitlementService.evaluateClaimant(claimant),
                MultipleClaimsWithSameNinoException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains(existingIds.toString());
    }

    @Test
    void shouldReturnEligibleWhenNotDuplicateAndEligibleWithVouchers() {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(duplicateClaimChecker.checkForDuplicateClaimsFromHousehold(any())).willReturn(ELIGIBLE);
        given(claimRepository.findLiveClaimsWithNino(any())).willReturn(emptyList());
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        Claimant claimant = aValidClaimant();

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateClaimant(claimant);

        assertCorrectResult(result, ELIGIBLE, eligibilityResponse, voucherEntitlement);
        verify(claimRepository).findLiveClaimsWithNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(duplicateClaimChecker).checkForDuplicateClaimsFromHousehold(eligibilityResponse);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                LocalDate.now());
    }

    @Test
    void shouldReturnIneligibleWhenNotDuplicateAndEligibleWithoutVouchers() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();
        given(claimRepository.findLiveClaimsWithNino(any())).willReturn(emptyList());
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(duplicateClaimChecker.checkForDuplicateClaimsFromHousehold(any())).willReturn(ELIGIBLE);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateClaimant(claimant);

        assertCorrectResult(result, INELIGIBLE, eligibilityResponse, voucherEntitlement);
        verify(claimRepository).findLiveClaimsWithNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(duplicateClaimChecker).checkForDuplicateClaimsFromHousehold(eligibilityResponse);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
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
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result =
                eligibilityAndEntitlementService.evaluateExistingClaimant(claimant, cycleStartDate, previousCycle);

        assertCorrectResult(result, ELIGIBLE, eligibilityResponse, voucherEntitlement);
        verify(client).checkEligibility(claimant);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        verifyZeroInteractions(duplicateClaimChecker, claimRepository);
    }

    @Test
    void shouldReturnIneligibleForExistingClaimantWithoutVouchers() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        LocalDate cycleStartDate = LocalDate.now().minusDays(1);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();
        PaymentCycle previousCycle = PaymentCycleTestDataFactory.aValidPaymentCycle();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result =
                eligibilityAndEntitlementService.evaluateExistingClaimant(claimant, cycleStartDate, previousCycle);

        assertCorrectResult(result, INELIGIBLE, eligibilityResponse, voucherEntitlement);
        verify(client).checkEligibility(claimant);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        verifyZeroInteractions(duplicateClaimChecker, claimRepository);
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
