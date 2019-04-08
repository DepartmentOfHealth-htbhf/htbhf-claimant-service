package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.aValidEligibilityResponseBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@ExtendWith(MockitoExtension.class)
public class ClaimServiceTest {

    @InjectMocks
    private ClaimService claimService;

    @Mock
    private ClaimantRepository claimantRepository;

    @Mock
    private EligibilityClient client;

    @Test
    public void shouldSaveNewClaimant() {
        //given
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(anEligibilityResponse());
        given(claimantRepository.eligibleClaimExistsForDwpHousehold(anyString())).willReturn(false);
        given(claimantRepository.eligibleClaimExistsForHmrcHousehold(anyString())).willReturn(false);
        Claimant claimant = aValidClaimantBuilder().build();

        //when
        claimService.createClaim(buildClaim(claimant));

        //then
        Claimant expectedClaimant = buildExpectedClaimant(claimant, ELIGIBLE);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(claimantRepository).save(expectedClaimant);
        verify(claimantRepository).eligibleClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());
        verify(claimantRepository).eligibleClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());
        verifyNoMoreInteractions(claimantRepository);
        verify(client).checkEligibility(claimant);
    }

    @Test
    public void shouldSaveDuplicateClaimantForMatchingNino() {
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(true);

        claimService.createClaim(buildClaim(claimant));

        Claimant expectedClaimant = buildExpectedClaimant(claimant, EligibilityStatus.DUPLICATE);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(claimantRepository).save(expectedClaimant);
        verifyNoMoreInteractions(claimantRepository);
        verifyZeroInteractions(client);
    }

    @Test
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    /**
     * This is a false positive. PMD can't follow the data flow of `claimant` inside the lambda.
     * https://github.com/pmd/pmd/issues/1304
     */
    public void shouldSaveClaimantWhenEligibilityThrowsException() {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        RuntimeException testException = new RuntimeException("Test exception");
        given(client.checkEligibility(any())).willThrow(testException);

        //when
        RuntimeException thrown = catchThrowableOfType(() -> claimService.createClaim(buildClaim(claimant)), RuntimeException.class);

        //then
        assertThat(thrown).isEqualTo(testException);
        Claimant expectedClaimant = buildExpectedClaimant(claimant, EligibilityStatus.ERROR);
        verify(claimantRepository).save(expectedClaimant);
        verify(client).checkEligibility(claimant);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verifyNoMoreInteractions(claimantRepository);
    }

    @Test
    public void shouldSaveDuplicateClaimantForMatchingDwpHouseholdIdentifier() {
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(claimantRepository.eligibleClaimExistsForDwpHousehold(anyString())).willReturn(true);
        given(client.checkEligibility(any()))
                .willReturn(buildEligibilityResponse(null, claimant.getDwpHouseholdIdentifier()));

        claimService.createClaim(buildClaim(claimant));

        Claimant expectedClaimant = buildExpectedClaimant(claimant, EligibilityStatus.DUPLICATE);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(claimantRepository).eligibleClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());
        verify(claimantRepository).save(expectedClaimant);
        verifyNoMoreInteractions(claimantRepository);
        verifyZeroInteractions(client);
    }

    @Test
    public void shouldSaveDuplicateClaimantForMatchingHmrcHouseholdIdentifier() {
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(claimantRepository.eligibleClaimExistsForHmrcHousehold(anyString())).willReturn(true);
        given(client.checkEligibility(any()))
                .willReturn(buildEligibilityResponse(claimant.getHmrcHouseholdIdentifier(), null));

        claimService.createClaim(buildClaim(claimant));

        Claimant expectedClaimant = buildExpectedClaimant(claimant, EligibilityStatus.DUPLICATE);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(claimantRepository).eligibleClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());
        verify(claimantRepository).save(expectedClaimant);
        verifyNoMoreInteractions(claimantRepository);
        verifyZeroInteractions(client);
    }

    @ParameterizedTest(name = "Should save duplicate claimant for matching household identifiers: DWP [{0}] HMRC [{1}]")
    @CsvSource({
            "true, true",
            "true, false",
            "false, true"
    })
    public void shouldSaveDuplicateClaimantForMatchingHouseholdIdentifiers(boolean matchingDwpHouseholdIdentifier, boolean matchingHmrcHouseholdIdentifier) {
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(claimantRepository.eligibleClaimExistsForDwpHousehold(anyString())).willReturn(matchingDwpHouseholdIdentifier);
        given(claimantRepository.eligibleClaimExistsForHmrcHousehold(anyString())).willReturn(matchingHmrcHouseholdIdentifier);
        given(client.checkEligibility(any())).willReturn(anEligibilityResponse());
        Claimant claimant = aValidClaimantBuilder().build();

        claimService.createClaim(buildClaim(claimant));

        Claimant expectedClaimant = buildExpectedClaimant(claimant, EligibilityStatus.DUPLICATE);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(claimantRepository).eligibleClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());
        verify(claimantRepository).eligibleClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());
        verify(claimantRepository).save(expectedClaimant);
        verifyNoMoreInteractions(claimantRepository);
        verifyZeroInteractions(client);
    }

    private EligibilityResponse buildEligibilityResponse(String hmrcHouseholdIdentifier, String dwpHouseholdIdentifier) {
        return aValidEligibilityResponseBuilder()
                .hmrcHouseholdIdentifier(hmrcHouseholdIdentifier)
                .dwpHouseholdIdentifier(dwpHouseholdIdentifier)
                .build();
    }

    private Claim buildClaim(Claimant claimant) {
        return Claim.builder().claimant(claimant).build();
    }

    private Claimant buildExpectedClaimant(Claimant claimant, EligibilityStatus eligibilityStatus) {
        return claimant.toBuilder().eligibilityStatus(eligibilityStatus).build();
    }
}
