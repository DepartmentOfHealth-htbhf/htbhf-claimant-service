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
import uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants;
import uk.gov.dhsc.htbhf.dwp.model.v2.EligibilityOutcome;
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.dwp.model.v2.VerificationOutcome;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithZeroVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.LISA_DOB;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.MAGGIE_DOB;
import static uk.gov.dhsc.htbhf.dwp.model.v2.EligibilityOutcome.CONFIRMED;
import static uk.gov.dhsc.htbhf.dwp.model.v2.EligibilityOutcome.NOT_CONFIRMED;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityNotConfirmedResponse;
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

        assertCorrectEligibleResult(decision, eligibilityResponse, voucherEntitlement);
        verify(claimRepository).findLiveClaimsWithNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                LocalDate.now());
        verifyNoInteractions(duplicateClaimChecker);
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
    void shouldReturnIneligibleWhenEligibilityServiceReportsIneligible() {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(INELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any())).willReturn(false);
        given(claimRepository.findLiveClaimsWithNino(any())).willReturn(emptyList());
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        Claimant claimant = aValidClaimant();

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateClaimant(claimant);

        assertCorrectIneligibleResultWithNoVoucherEntitlement(result, NOT_CONFIRMED, eligibilityResponse);
        verify(claimRepository).findLiveClaimsWithNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(eligibilityResponse);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                LocalDate.now());
    }

    @Test
    void shouldReturnEligibleWhenNotDuplicateAndEligibleWithVouchers() {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any())).willReturn(false);
        given(claimRepository.findLiveClaimsWithNino(any())).willReturn(emptyList());
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        Claimant claimant = aValidClaimant();

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateClaimant(claimant);

        assertCorrectEligibleResult(result, eligibilityResponse, voucherEntitlement);
        verify(claimRepository).findLiveClaimsWithNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(eligibilityResponse);
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
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any())).willReturn(false);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateClaimant(claimant);

        assertCorrectIneligibleResultWithNoVoucherEntitlement(result, CONFIRMED, eligibilityResponse);
        verify(claimRepository).findLiveClaimsWithNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(eligibilityResponse);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                LocalDate.now());
    }

    @Test
    void shouldReturnIneligibleWhenIneligibleWithDwp() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(INELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        given(claimRepository.findLiveClaimsWithNino(any())).willReturn(emptyList());
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any())).willReturn(false);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateClaimant(claimant);

        assertCorrectIneligibleResultWithNoVoucherEntitlement(result, NOT_CONFIRMED, eligibilityResponse);
        verify(claimRepository).findLiveClaimsWithNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(eligibilityResponse);
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

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateExistingClaimant(claimant, cycleStartDate, previousCycle);

        assertCorrectEligibleResult(result, eligibilityResponse, voucherEntitlement);
        verify(client).checkEligibility(claimant);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        verifyNoInteractions(duplicateClaimChecker, claimRepository);
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

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateExistingClaimant(claimant, cycleStartDate, previousCycle);

        assertCorrectIneligibleResultWithNoVoucherEntitlement(result, CONFIRMED, eligibilityResponse);
        verify(client).checkEligibility(claimant);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        verifyNoInteractions(duplicateClaimChecker, claimRepository);
    }

    @Test
    void shouldReturnDuplicateForExistingHousehold() {
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(claimRepository.findLiveClaimsWithNino(any())).willReturn(emptyList());
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        Claimant claimant = aValidClaimant();
        given(duplicateClaimChecker.liveClaimExistsForHousehold(eligibilityResponse)).willReturn(true);

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateClaimant(claimant);

        assertCorrectDuplicateResult(result, eligibilityResponse, voucherEntitlement);
        verify(client).checkEligibility(claimant);
    }

    @Test
    void shouldReturnIneligibleForExistingClaimantThatBecomesIneligibleWithDwp() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(INELIGIBLE);
        LocalDate cycleStartDate = LocalDate.now().minusDays(1);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        PaymentCycle previousCycle = PaymentCycleTestDataFactory.aValidPaymentCycle();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(voucherEntitlement);

        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateExistingClaimant(claimant, cycleStartDate, previousCycle);

        assertCorrectIneligibleResultWithNoVoucherEntitlement(result, NOT_CONFIRMED, eligibilityResponse);
        verify(client).checkEligibility(claimant);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()),
                eligibilityResponse.getDateOfBirthOfChildren(),
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        verifyNoInteractions(duplicateClaimChecker, claimRepository);
    }

    private void assertCorrectEligibleResult(EligibilityAndEntitlementDecision result,
                                             EligibilityResponse eligibilityResponse,
                                             PaymentCycleVoucherEntitlement voucherEntitlement) {
        assertDecisionResultCorrectApartFromVoucherEntitlement(result, ELIGIBLE, CONFIRMED, eligibilityResponse);
        assertThat(result.getVoucherEntitlement()).isEqualTo(voucherEntitlement);
    }

    private void assertCorrectDuplicateResult(EligibilityAndEntitlementDecision result,
                                              EligibilityResponse eligibilityResponse,
                                              PaymentCycleVoucherEntitlement voucherEntitlement) {
        assertDecisionResultCorrectApartFromVoucherEntitlement(result, DUPLICATE, CONFIRMED, eligibilityResponse);
        assertThat(result.getVoucherEntitlement()).isEqualTo(voucherEntitlement);
    }

    private void assertCorrectIneligibleResultWithNoVoucherEntitlement(EligibilityAndEntitlementDecision result,
                                                                       EligibilityOutcome eligibilityOutcome,
                                                                       EligibilityResponse eligibilityResponse) {
        assertDecisionResultCorrectApartFromVoucherEntitlement(result, INELIGIBLE, eligibilityOutcome, eligibilityResponse);
        assertThat(result.getVoucherEntitlement()).isNull();
    }

    private void assertDecisionResultCorrectApartFromVoucherEntitlement(EligibilityAndEntitlementDecision result,
                                                                        EligibilityStatus eligibilityStatus,
                                                                        EligibilityOutcome eligibilityOutcome,
                                                                        EligibilityResponse eligibilityResponse) {
        assertThat(result).isNotNull();
        assertThat(result.getEligibilityStatus()).isEqualTo(eligibilityStatus);
        List<LocalDate> childrenDobs = List.of(MAGGIE_DOB, LISA_DOB);
        IdentityAndEligibilityResponse identityAndEligibilityResponse = buildIdentityAndEligibilityResponse(childrenDobs, eligibilityOutcome);
        assertThat(result.getIdentityAndEligibilityResponse()).isEqualTo(identityAndEligibilityResponse);
        assertThat(result.getDateOfBirthOfChildren()).isEqualTo(eligibilityResponse.getDateOfBirthOfChildren());
        assertThat(result.getDwpHouseholdIdentifier()).isEqualTo(eligibilityResponse.getDwpHouseholdIdentifier());
        assertThat(result.getHmrcHouseholdIdentifier()).isEqualTo(eligibilityResponse.getHmrcHouseholdIdentifier());
    }

    private IdentityAndEligibilityResponse buildIdentityAndEligibilityResponse(List<LocalDate> childrenDobs, EligibilityOutcome eligibilityOutcome) {
        IdentityAndEligibilityResponse identityAndEligibilityResponse = (CONFIRMED == eligibilityOutcome)
                ? anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(VerificationOutcome.NOT_SUPPLIED, childrenDobs)
                : anIdentityMatchedEligibilityNotConfirmedResponse();
        return identityAndEligibilityResponse.toBuilder()
                .householdIdentifier(TestConstants.DWP_HOUSEHOLD_IDENTIFIER)
                .build();
    }

}
