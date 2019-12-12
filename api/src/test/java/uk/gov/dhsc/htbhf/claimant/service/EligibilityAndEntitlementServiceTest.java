package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_NINO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@ExtendWith(MockitoExtension.class)
class EligibilityAndEntitlementServiceTest {

    private static final boolean NOT_DUPLICATE = false;
    private static final boolean DUPLICATE = true;
    private static final CombinedIdentityAndEligibilityResponse IDENTITY_AND_ELIGIBILITY_RESPONSE =
            CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
    private static final List<LocalDate> DATE_OF_BIRTH_OF_CHILDREN = IDENTITY_AND_ELIGIBILITY_RESPONSE.getDobOfChildrenUnder4();
    private static final PaymentCycleVoucherEntitlement VOUCHER_ENTITLEMENT = aPaymentCycleVoucherEntitlementWithVouchers();
    private static final Claimant CLAIMANT = aValidClaimant();
    private static final UUID NO_EXISTING_CLAIM = null;

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

    @Test
    void shouldReturnDuplicateWhenLiveClaimAlreadyExists() {
        //Given
        UUID existingClaimId = UUID.randomUUID();
        given(claimRepository.findLiveClaimWithNino(any())).willReturn(Optional.of(existingClaimId));

        //When
        EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateNewClaimant(CLAIMANT);

        //Then
        assertThat(decision.getEligibilityStatus()).isEqualTo(EligibilityStatus.DUPLICATE);
        assertThat(decision.getExistingClaimId()).isEqualTo(existingClaimId);
        verifyNoInteractions(client);
    }

    @Test
    void shouldReturnDuplicateForExistingHousehold() {
        //Given
        setupCommonMocks(NO_EXISTING_CLAIM);
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any(CombinedIdentityAndEligibilityResponse.class))).willReturn(DUPLICATE);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(EligibilityStatus.DUPLICATE);

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateNewClaimant(CLAIMANT);

        //Then
        assertThat(result).isEqualTo(decisionResponse);
        verifyCommonMocks(DUPLICATE);
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(IDENTITY_AND_ELIGIBILITY_RESPONSE);
    }

    @Test
    void shouldReturnEligibleWhenNotDuplicateAndEligible() {
        //Given
        setupCommonMocks(NO_EXISTING_CLAIM);
        given(duplicateClaimChecker.liveClaimExistsForHousehold(any(CombinedIdentityAndEligibilityResponse.class))).willReturn(NOT_DUPLICATE);
        EligibilityAndEntitlementDecision decisionResponse = setupEligibilityAndEntitlementDecisionFactory(ELIGIBLE);

        //When
        EligibilityAndEntitlementDecision result = eligibilityAndEntitlementService.evaluateNewClaimant(CLAIMANT);

        //Then
        assertThat(result).isEqualTo(decisionResponse);
        verifyCommonMocks(NOT_DUPLICATE);
        verify(duplicateClaimChecker).liveClaimExistsForHousehold(IDENTITY_AND_ELIGIBILITY_RESPONSE);
    }

    private EligibilityAndEntitlementDecision setupEligibilityAndEntitlementDecisionFactory(EligibilityStatus status) {
        EligibilityAndEntitlementDecision decisionResponse = aDecisionWithStatus(status);
        given(eligibilityAndEntitlementDecisionFactory.buildDecision(any(), any(), anyBoolean())).willReturn(decisionResponse);
        return decisionResponse;
    }

    private void setupCommonMocks(UUID existingClaimId) {
        given(client.checkIdentityAndEligibility(any())).willReturn(IDENTITY_AND_ELIGIBILITY_RESPONSE);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any())).willReturn(VOUCHER_ENTITLEMENT);
        given(claimRepository.findLiveClaimWithNino(any())).willReturn(Optional.ofNullable(existingClaimId));
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
