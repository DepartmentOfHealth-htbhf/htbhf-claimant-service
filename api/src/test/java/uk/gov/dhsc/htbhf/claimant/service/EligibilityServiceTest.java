package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    @InjectMocks
    private EligibilityService eligibilityService;

    @Mock
    private EligibilityClient client;

    @Mock
    private EligibilityStatusCalculator eligibilityStatusCalculator;

    @Mock
    private ClaimRepository claimRepository;

    @Test
    void shouldReturnDuplicateResponseWhenLiveClaimAlreadyExists() {
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(true);
        Claimant claimant = aValidClaimant();

        EligibilityResponse eligibilityResponse = eligibilityService.determineEligibility(claimant);

        assertThat(eligibilityResponse.getEligibilityStatus()).isEqualTo(DUPLICATE);
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verifyZeroInteractions(client);
        verifyZeroInteractions(eligibilityStatusCalculator);
    }

    @Test
    void shouldReturnEligibilityResponseFromClientWhenNoLiveClaimExists() {
        Claimant claimant = aValidClaimant();
        EligibilityResponse eligibilityResponse = anEligibilityResponse();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(ELIGIBLE);

        EligibilityResponse result = eligibilityService.determineEligibility(claimant);

        assertThat(result).isEqualTo(eligibilityResponse);
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verify(client).checkEligibility(claimant);
        verify(eligibilityStatusCalculator).determineEligibilityStatus(eligibilityResponse);
    }

}
