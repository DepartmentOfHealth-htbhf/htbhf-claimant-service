package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.dwp.model.DeathVerificationFlag;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;
import uk.gov.dhsc.htbhf.dwp.model.IdentityOutcome;
import uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_NINO;
import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_AND_LISA_DOBS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

@ExtendWith(MockitoExtension.class)
class EligibilityAndEntitlementServiceTest {

    private static final boolean NOT_DUPLICATE = false;
    private static final boolean DUPLICATE = true;
    private  static final EligibilityOutcome NO_ELIGIBILITY_OVERRIDE = null;
    private static final CombinedIdentityAndEligibilityResponse IDENTITY_AND_ELIGIBILITY_RESPONSE = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
    private static final List<LocalDate> DATE_OF_BIRTH_OF_CHILDREN = IDENTITY_AND_ELIGIBILITY_RESPONSE.getDobOfChildrenUnder4();
    private static final PaymentCycleVoucherEntitlement VOUCHER_ENTITLEMENT = aPaymentCycleVoucherEntitlementWithVouchers();
    private static final Claimant CLAIMANT = aValidClaimant();

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

    @Mock
    EligibilityAndEntitlementDecisionFactory eligibilityAndEntitlementDecisionFactory;

    @ParameterizedTest
    @MethodSource("allEligibilityOutcomeValues")
    void shouldReturnDuplicateWhenLiveClaimAlreadyExists(EligibilityOutcome eligibilityOverrideOutcome) {
        //Given
        UUID existingClaimId = UUID.randomUUID();
        given(claimRepository.findLiveClaimWithNino(any())).willReturn(Optional.of(existingClaimId));

        //When
        EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateNewClaimant(CLAIMANT, eligibilityOverrideOutcome);

        //Then
        assertThat(decision.getEligibilityStatus()).isEqualTo(EligibilityStatus.DUPLICATE);
        assertThat(decision.getExistingClaimId()).isEqualTo(existingClaimId);
        verifyNoInteractions(client);
    }

    private static Stream<EligibilityOutcome> allEligibilityOutcomeValues() {
        return Stream.of(
                null,
                EligibilityOutcome.CONFIRMED,
                EligibilityOutcome.NOT_CONFIRMED
        );
    }

    @Test
    void shouldReturnDuplicateForExistingHousehold() {
        //Given
        setupCommonMocks();
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any(CombinedIdentityAndEligibilityResponse.class))).willReturn(DUPLICATE);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(EligibilityStatus.DUPLICATE);

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateNewClaimant(CLAIMANT, NO_ELIGIBILITY_OVERRIDE);

        //Then
        assertThat(result).isEqualTo(decisionResponse);
        verifyCommonMocks(DUPLICATE);
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(IDENTITY_AND_ELIGIBILITY_RESPONSE);
    }

    @Test
    void shouldReturnEligibleWhenNotDuplicateAndEligible() {
        //Given
        setupCommonMocks();
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any(CombinedIdentityAndEligibilityResponse.class))).willReturn(NOT_DUPLICATE);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(ELIGIBLE);

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateNewClaimant(CLAIMANT, NO_ELIGIBILITY_OVERRIDE);

        //Then
        assertThat(result).isEqualTo(decisionResponse);
        verifyCommonMocks(NOT_DUPLICATE);
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(IDENTITY_AND_ELIGIBILITY_RESPONSE);
    }

    @Test
    void shouldReturnEligibilityOverride() {
        //Given
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(VOUCHER_ENTITLEMENT);
        given(claimRepository.findLiveClaimWithNino(any())).willReturn(Optional.empty());
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any(CombinedIdentityAndEligibilityResponse.class))).willReturn(NOT_DUPLICATE);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(ELIGIBLE);

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateNewClaimant(CLAIMANT, EligibilityOutcome.CONFIRMED);

        //Then
        CombinedIdentityAndEligibilityResponse response = CombinedIdentityAndEligibilityResponse.builder()
                .identityStatus(IdentityOutcome.MATCHED)
                .eligibilityStatus(EligibilityOutcome.CONFIRMED)
                .addressLine1Match(VerificationOutcome.NOT_SET)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .dobOfChildrenUnder4(Collections.emptyList())
                .emailAddressMatch(VerificationOutcome.NOT_SET)
                .mobilePhoneMatch(VerificationOutcome.NOT_SET)
                .postcodeMatch(VerificationOutcome.NOT_SET)
                .pregnantChildDOBMatch(VerificationOutcome.MATCHED)
                .build();

        assertThat(result).isEqualTo(decisionResponse);
        verify(eligibilityAndEntitlementDecisionFactory).buildDecision(eq(response), any(), eq(NOT_DUPLICATE));
    }

    @Test
    void shouldEvaluateClaimForGivenPaymentCycle() {
        //Given
        PaymentCycle paymentCycle = aValidPaymentCycle();
        EligibilityAndEntitlementDecision decision = anEligibleDecision();
        given(client.checkIdentityAndEligibility(any())).willReturn(IDENTITY_AND_ELIGIBILITY_RESPONSE);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(VOUCHER_ENTITLEMENT);
        given(eligibilityAndEntitlementDecisionFactory.buildDecision(any(), any(), anyBoolean())).willReturn(decision);
        Claimant claimant = paymentCycle.getClaim().getClaimant();
        LocalDate cycleStartDate = LocalDate.now();

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateClaimantForPaymentCycle(claimant, cycleStartDate, paymentCycle);

        //Then
        assertThat(result).isEqualTo(decision);
        verify(client).checkIdentityAndEligibility(claimant);
        verify(paymentCycleEntitlementCalculator)
                .calculateEntitlement(Optional.of(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS), MAGGIE_AND_LISA_DOBS, cycleStartDate, VOUCHER_ENTITLEMENT);
        verify(eligibilityAndEntitlementDecisionFactory).buildDecision(IDENTITY_AND_ELIGIBILITY_RESPONSE, VOUCHER_ENTITLEMENT, false);
    }

    private EligibilityAndEntitlementDecision setupEligibilityAndEntitlementDecisionFactory(EligibilityStatus status) {
        EligibilityAndEntitlementDecision decisionResponse = aDecisionWithStatus(status);
        given(eligibilityAndEntitlementDecisionFactory.buildDecision(any(), any(), anyBoolean())).willReturn(decisionResponse);
        return decisionResponse;
    }

    private void setupCommonMocks() {
        given(client.checkIdentityAndEligibility(any())).willReturn(IDENTITY_AND_ELIGIBILITY_RESPONSE);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(VOUCHER_ENTITLEMENT);
        given(claimRepository.findLiveClaimWithNino(any())).willReturn(Optional.empty());
    }

    private void verifyCommonMocks(boolean duplicate) {
        verify(claimRepository).findLiveClaimWithNino(HOMER_NINO);
        verify(client).checkIdentityAndEligibility(CLAIMANT);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.of(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS),
                DATE_OF_BIRTH_OF_CHILDREN,
                LocalDate.now());
        verify(eligibilityAndEntitlementDecisionFactory).buildDecision(IDENTITY_AND_ELIGIBILITY_RESPONSE, VOUCHER_ENTITLEMENT, duplicate);
    }

}
