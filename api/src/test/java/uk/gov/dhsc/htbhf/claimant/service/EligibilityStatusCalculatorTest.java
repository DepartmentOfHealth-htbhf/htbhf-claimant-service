package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;

@ExtendWith(MockitoExtension.class)
class EligibilityStatusCalculatorTest {

    @InjectMocks
    private EligibilityStatusCalculator eligibilityStatusCalculator;

    @Mock
    private ClaimRepository claimRepository;

    @ParameterizedTest(name = "Should return duplicate status for matching household identifiers: DWP [{0}] HMRC [{1}]")
    @CsvSource({
            "true, true",
            "true, false",
            "false, true"
    })
    void shouldReturnDuplicateStatusForMatchingHouseholds(boolean matchingDwpHouseholdIdentifier, boolean matchingHmrcHouseholdIdentifier) {
        given(claimRepository.liveClaimExistsForDwpHousehold(anyString())).willReturn(matchingDwpHouseholdIdentifier);
        given(claimRepository.liveClaimExistsForHmrcHousehold(anyString())).willReturn(matchingHmrcHouseholdIdentifier);

        EligibilityStatus status = eligibilityStatusCalculator.determineEligibilityStatus(anEligibilityResponseWithStatus(null));

        assertThat(status).isEqualTo(EligibilityStatus.DUPLICATE);
        verify(claimRepository).liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);
        verify(claimRepository).liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    @Test
    void shouldReturnStatusFromEligibilityServiceWhenNoMatchingHousehold() {
        EligibilityStatus status = EligibilityStatus.PENDING;
        given(claimRepository.liveClaimExistsForDwpHousehold(anyString())).willReturn(false);
        given(claimRepository.liveClaimExistsForHmrcHousehold(anyString())).willReturn(false);

        EligibilityStatus result = eligibilityStatusCalculator.determineEligibilityStatus(anEligibilityResponseWithStatus(status));

        assertThat(result).isEqualTo(status);
        verify(claimRepository).liveClaimExistsForDwpHousehold(DWP_HOUSEHOLD_IDENTIFIER);
        verify(claimRepository).liveClaimExistsForHmrcHousehold(HMRC_HOUSEHOLD_IDENTIFIER);
    }
}
