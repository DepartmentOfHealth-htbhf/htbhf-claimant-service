package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

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
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(anEligibilityResponse());

        //when
        claimService.createClaim(buildClaim(claimant));

        //then
        Claimant expectedClaimant = claimant
                .toBuilder()
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .dwpHouseholdIdentifier("dwpHousehold1")
                .hmrcHouseholdIdentifier("hmrcHousehold1")
                .build();
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(claimantRepository).save(expectedClaimant);
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
    }

    @Test
    public void shouldSaveDuplicateClaimantForMatchingDwpHouseholdIdentifier() {
        Claimant claimant = aValidClaimantBuilder().build();
        EligibilityResponse eligibilityResponse = aValidEligibilityResponseBuilder()
                .hmrcHouseholdIdentifier(null)
                .dwpHouseholdIdentifier(claimant.getDwpHouseholdIdentifier())
                .build();
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(claimantRepository.eligibleClaimExistsForDwpHousehold(anyString())).willReturn(true);
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);

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
        EligibilityResponse eligibilityResponse = aValidEligibilityResponseBuilder()
                .hmrcHouseholdIdentifier(claimant.getHmrcHouseholdIdentifier())
                .dwpHouseholdIdentifier(null)
                .build();
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(claimantRepository.eligibleClaimExistsForHmrcHousehold(anyString())).willReturn(true);
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);

        claimService.createClaim(buildClaim(claimant));

        Claimant expectedClaimant = buildExpectedClaimant(claimant, EligibilityStatus.DUPLICATE);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(claimantRepository).eligibleClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());
        verify(claimantRepository).save(expectedClaimant);
        verifyNoMoreInteractions(claimantRepository);
        verifyZeroInteractions(client);
    }

    private Claim buildClaim(Claimant claimant) {
        return Claim.builder().claimant(claimant).build();
    }

    private Claimant buildExpectedClaimant(Claimant claimant, EligibilityStatus duplicate) {
        return claimant.toBuilder().eligibilityStatus(duplicate).build();
    }
}
