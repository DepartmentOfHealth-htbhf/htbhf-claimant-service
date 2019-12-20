package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

@ExtendWith(MockitoExtension.class)
class DuplicateClaimCheckerTest {

    private static final CombinedIdentityAndEligibilityResponse ELIGIBLE_RESPONSE = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();

    @InjectMocks
    private DuplicateClaimChecker duplicateClaimChecker;

    @Mock
    private ClaimRepository claimRepository;

    @ParameterizedTest(name = "Should return duplicate status for matching household identifiers: DWP [{0}] HMRC [{1}]")
    @CsvSource({
            "true, true",
            "true, false",
            "false, true"
    })
    void shouldReturnTrueForMatchingHouseholdsInEligibilityResponse(boolean matchingDwpHouseholdIdentifier, boolean matchingHmrcHouseholdIdentifier) {
        given(claimRepository.liveClaimExistsForDwpHousehold(anyString())).willReturn(matchingDwpHouseholdIdentifier);
        given(claimRepository.liveClaimExistsForHmrcHousehold(anyString())).willReturn(matchingHmrcHouseholdIdentifier);

        boolean result = duplicateClaimChecker.liveClaimExistsForHousehold(anEligibilityResponseWithStatus(null));

        assertThat(result).isTrue();
        verify(claimRepository).liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);
        verify(claimRepository).liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    @ParameterizedTest(name = "Should return duplicate status for matching household identifiers: DWP [{0}] HMRC [{1}]")
    @CsvSource({
            "true, true",
            "true, false",
            "false, true"
    })
    void shouldReturnTrueForMatchingHouseholdsInCombinedResponse(boolean matchingDwpHouseholdIdentifier, boolean matchingHmrcHouseholdIdentifier) {
        given(claimRepository.liveClaimExistsForDwpHousehold(anyString())).willReturn(matchingDwpHouseholdIdentifier);
        given(claimRepository.liveClaimExistsForHmrcHousehold(anyString())).willReturn(matchingHmrcHouseholdIdentifier);

        boolean result = duplicateClaimChecker.liveClaimExistsForHousehold(ELIGIBLE_RESPONSE);

        assertThat(result).isTrue();
        verify(claimRepository).liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);
        verify(claimRepository).liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    @Test
    void shouldReturnFalseWhenNoMatchingHouseholdForEligibilityResponse() {
        given(claimRepository.liveClaimExistsForDwpHousehold(anyString())).willReturn(false);
        given(claimRepository.liveClaimExistsForHmrcHousehold(anyString())).willReturn(false);

        boolean result = duplicateClaimChecker.liveClaimExistsForHousehold(anEligibilityResponseWithStatus(EligibilityStatus.ELIGIBLE));

        assertThat(result).isFalse();
        verify(claimRepository).liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);
        verify(claimRepository).liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    @Test
    void shouldReturnFalseWhenNoMatchingHouseholdForCombinedResponse() {
        given(claimRepository.liveClaimExistsForDwpHousehold(anyString())).willReturn(false);
        given(claimRepository.liveClaimExistsForHmrcHousehold(anyString())).willReturn(false);

        boolean result = duplicateClaimChecker.liveClaimExistsForHousehold(ELIGIBLE_RESPONSE);

        assertThat(result).isFalse();
        verify(claimRepository).liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);
        verify(claimRepository).liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    @Test
    void shouldReturnTrueForMatchingDwpHousehold() {
        given(claimRepository.liveClaimExistsForDwpHousehold(anyString())).willReturn(true);

        boolean result = duplicateClaimChecker.liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);

        assertThat(result).isTrue();
        verify(claimRepository).liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);
    }

    @Test
    void shouldReturnFalseWhenNoMatchingDwpHousehold() {
        given(claimRepository.liveClaimExistsForDwpHousehold(anyString())).willReturn(false);

        boolean result = duplicateClaimChecker.liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);

        assertThat(result).isFalse();
        verify(claimRepository).liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);
    }

    @Test
    void shouldReturnFalseWhenNoDwpHouseholdIdentifier() {
        boolean result = duplicateClaimChecker.liveClaimExistsForDwpHousehold(null);

        assertThat(result).isFalse();
        verifyNoInteractions(claimRepository);
    }

    @Test
    void shouldReturnTrueForMatchingHmrcHousehold() {
        given(claimRepository.liveClaimExistsForHmrcHousehold(anyString())).willReturn(true);

        boolean result = duplicateClaimChecker.liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);

        assertThat(result).isTrue();
        verify(claimRepository).liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    @Test
    void shouldReturnFalseWhenNoMatchingHmrcHousehold() {
        given(claimRepository.liveClaimExistsForHmrcHousehold(anyString())).willReturn(false);

        boolean result = duplicateClaimChecker.liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);

        assertThat(result).isFalse();
        verify(claimRepository).liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    @Test
    void shouldReturnFalseWhenNoHmrcHouseholdIdentifier() {
        boolean result = duplicateClaimChecker.liveClaimExistsForHmrcHousehold(null);

        assertThat(result).isFalse();
        verifyNoInteractions(claimRepository);
    }
}
