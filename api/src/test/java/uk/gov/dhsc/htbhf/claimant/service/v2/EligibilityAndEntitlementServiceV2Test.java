package uk.gov.dhsc.htbhf.claimant.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.DuplicateClaimChecker;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementDecisionFactory;
import uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_NINO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class EligibilityAndEntitlementServiceV2Test {

    private static final boolean NOT_DUPLICATE = false;
    private static final boolean DUPLICATE = true;
    private static final EligibilityResponse ELIGIBILITY_RESPONSE = anEligibilityResponseWithStatus(ELIGIBLE);
    private static final List<LocalDate> DATE_OF_BIRTH_OF_CHILDREN = ELIGIBILITY_RESPONSE.getDateOfBirthOfChildren();
    private static final CombinedIdentityAndEligibilityResponse COMBINED_ELIGIBILITY_RESPONSE = CombinedIdAndEligibilityResponseTestDataFactory
            .anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(DATE_OF_BIRTH_OF_CHILDREN);
    private static final PaymentCycleVoucherEntitlement VOUCHER_ENTITLEMENT = aPaymentCycleVoucherEntitlementWithVouchers();
    private static final Claimant CLAIMANT = aValidClaimant();

    @InjectMocks
    EligibilityAndEntitlementServiceV2 eligibilityAndEntitlementServiceV2;

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

    @Test
    void shouldReturnExistingClaimIdWhenLiveClaimAlreadyExists() {
        //Given
        UUID existingClaimId = UUID.randomUUID();
        setupCommonMocks(existingClaimId);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(INELIGIBLE);

        //When
        EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementServiceV2.evaluateNewClaimant(CLAIMANT);

        //Then
        assertThat(decision).isEqualTo(decisionResponse);
        verifyCommonMocks();
        verify(eligibilityAndEntitlementDecisionFactory).buildDecision(COMBINED_ELIGIBILITY_RESPONSE, VOUCHER_ENTITLEMENT, existingClaimId, NOT_DUPLICATE);
        verifyNoInteractions(duplicateClaimChecker);
    }

    @Test
    void shouldReturnEligibleWhenNotDuplicateAndEligible() {
        //Given
        setupCommonMocks(null);
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any(EligibilityResponse.class))).willReturn(false);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(ELIGIBLE);

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementServiceV2.evaluateNewClaimant(CLAIMANT);

        //Then
        assertThat(result).isEqualTo(decisionResponse);
        verifyCommonMocks();
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(ELIGIBILITY_RESPONSE);
        verify(eligibilityAndEntitlementDecisionFactory).buildDecision(COMBINED_ELIGIBILITY_RESPONSE, VOUCHER_ENTITLEMENT, null, NOT_DUPLICATE);
    }

    @Test
    void shouldReturnDuplicateForExistingHousehold() {
        //Given
        setupCommonMocks(null);
        given(duplicateClaimChecker.liveClaimExistsForHousehold(ELIGIBILITY_RESPONSE)).willReturn(DUPLICATE);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(EligibilityStatus.DUPLICATE);

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementServiceV2.evaluateNewClaimant(CLAIMANT);

        //Then
        assertThat(result).isEqualTo(decisionResponse);
        verifyCommonMocks();
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(ELIGIBILITY_RESPONSE);
        verify(eligibilityAndEntitlementDecisionFactory).buildDecision(COMBINED_ELIGIBILITY_RESPONSE, VOUCHER_ENTITLEMENT, null, DUPLICATE);
    }

    @Test
    void shouldReturnEligibleForExistingClaimantWithVouchers() {
        //Given
        PaymentCycle previousCycle = PaymentCycleTestDataFactory.aValidPaymentCycle();
        given(client.checkEligibility(any())).willReturn(ELIGIBILITY_RESPONSE);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(VOUCHER_ENTITLEMENT);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(ELIGIBLE);
        LocalDate cycleStartDate = LocalDate.now().minusDays(1);

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementServiceV2.evaluateClaimantForPaymentCycle(CLAIMANT, cycleStartDate, previousCycle);

        //Then
        assertThat(result).isEqualTo(decisionResponse);
        verify(client).checkEligibility(CLAIMANT);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.of(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS),
                DATE_OF_BIRTH_OF_CHILDREN,
                cycleStartDate,
                previousCycle.getVoucherEntitlement());
        verify(eligibilityAndEntitlementDecisionFactory).buildDecision(COMBINED_ELIGIBILITY_RESPONSE, VOUCHER_ENTITLEMENT, null, NOT_DUPLICATE);
        verifyNoInteractions(duplicateClaimChecker, claimRepository);
    }

    private EligibilityAndEntitlementDecision setupEligibilityAndEntitlementDecisionFactory(EligibilityStatus status) {
        EligibilityAndEntitlementDecision decisionResponse = aDecisionWithStatus(status);
        given(eligibilityAndEntitlementDecisionFactory.buildDecision(any(), any(), any(), anyBoolean())).willReturn(decisionResponse);
        return decisionResponse;
    }

    private void setupCommonMocks(UUID existingClaimId) {
        given(client.checkEligibility(any())).willReturn(ELIGIBILITY_RESPONSE);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(VOUCHER_ENTITLEMENT);
        given(claimRepository.findLiveClaimWithNino(any())).willReturn(Optional.ofNullable(existingClaimId));
    }

    private void verifyCommonMocks() {
        verify(claimRepository).findLiveClaimWithNino(HOMER_NINO);
        verify(client).checkEligibility(CLAIMANT);
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                Optional.of(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS),
                DATE_OF_BIRTH_OF_CHILDREN,
                LocalDate.now());
    }

}
