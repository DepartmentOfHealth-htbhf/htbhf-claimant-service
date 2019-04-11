package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.EntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

@ExtendWith(MockitoExtension.class)
class NewClaimServiceTest {

    @InjectMocks
    NewClaimService newClaimService;

    @Mock
    ClaimRepository claimRepository;

    @Mock
    EligibilityClient client;

    @Mock
    EligibilityStatusCalculator eligibilityStatusCalculator;

    @Mock
    EntitlementCalculator entitlementCalculator;

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @ParameterizedTest(name = "Should save claimant with claim status set to {1} when eligibility status is {0}")
    @CsvSource({
            "ELIGIBLE, NEW",
            "PENDING, PENDING",
            "NO_MATCH, REJECTED",
            "ERROR, ERROR",
            "INELIGIBLE, REJECTED"
    })
    void shouldSaveNonExistingClaimant(EligibilityStatus eligibilityStatus, ClaimStatus claimStatus) {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        EligibilityResponse eligibilityResponse = anEligibilityResponse();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(eligibilityStatus);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any())).willReturn(voucherEntitlement);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getClaimStatus()).isEqualTo(claimStatus);
        assertThat(result.getClaim().getEligibilityStatus()).isEqualTo(eligibilityStatus);
        assertThat(result.getVoucherEntitlement()).isEqualTo(voucherEntitlement);

        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verify(eligibilityStatusCalculator).determineEligibilityStatus(eligibilityResponse);
        verify(claimRepository).save(result.getClaim());
        verifyNoMoreInteractions(claimRepository);
        verify(client).checkEligibility(claimant);
    }

    @Test
    void shouldCorrectlyCalculateVoucherEntitlement() {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        EligibilityResponse eligibilityResponse = anEligibilityResponse();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(EligibilityStatus.ELIGIBLE);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any())).willReturn(voucherEntitlement);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherEntitlement()).isEqualTo(voucherEntitlement);

        verify(entitlementCalculator).calculateVoucherEntitlement(claimant, eligibilityResponse);
    }

    /**
     * Asserts that all eligibility statuses are mapped to a non null claim status.
     * @param eligibilityStatus the eligibility status to test with
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @ParameterizedTest(name = "Should save claimant with non null claim status for all eligibility statuses")
    @EnumSource(EligibilityStatus.class)
    void shouldSaveClaimantWithClaimStatus(EligibilityStatus eligibilityStatus) {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(anEligibilityResponse());
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(eligibilityStatus);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any())).willReturn(voucherEntitlement);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        verify(claimRepository).save(result.getClaim());
        assertThat(result.getClaim().getClaimStatus()).isNotNull();
    }

    @Test
    void shouldSaveDuplicateClaimantForMatchingNino() {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(true);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any())).willReturn(voucherEntitlement);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verify(claimRepository).save(result.getClaim());
        verifyNoMoreInteractions(claimRepository);
        verifyZeroInteractions(client);
    }

    /**
     * This is a false positive. PMD can't follow the data flow of `claimant` inside the lambda.
     * https://github.com/pmd/pmd/issues/1304
     */
    @Test
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    void shouldSaveClaimantWhenEligibilityThrowsException() {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        RuntimeException testException = new RuntimeException("Test exception");
        given(client.checkEligibility(any())).willThrow(testException);

        //when
        RuntimeException thrown = catchThrowableOfType(() -> newClaimService.createClaim(claimant), RuntimeException.class);

        //then
        assertThat(thrown).isEqualTo(testException);
        verify(claimRepository).save(any(Claim.class));
        verify(client).checkEligibility(claimant);
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verifyNoMoreInteractions(claimRepository);
    }
}
