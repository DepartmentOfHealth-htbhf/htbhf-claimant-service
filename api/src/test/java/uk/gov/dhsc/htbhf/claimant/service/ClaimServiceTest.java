package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;

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
        Claim claim = Claim.builder()
                .claimant(claimant)
                .build();
        given(claimantRepository.eligibleClaimExists(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(anEligibilityResponseWithStatus(ELIGIBLE));

        //when
        claimService.createClaim(claim);

        //then
        Claimant expectedClaimant = claimant
                .toBuilder()
                .eligibilityStatus(ELIGIBLE)
                .dwpHouseholdIdentifier("dwpHousehold1")
                .hmrcHouseholdIdentifier("hmrcHousehold1")
                .build();
        verify(claimantRepository).eligibleClaimExists(claimant.getNino());
        verify(claimantRepository).save(expectedClaimant);
        verify(client).checkEligibility(claimant);
    }

    @Test
    public void shouldSaveDuplicateClaimant() {
        Claimant claimant = aValidClaimantBuilder().build();
        Claim claim = Claim.builder()
                .claimant(claimant)
                .build();
        given(claimantRepository.eligibleClaimExists(any())).willReturn(true);

        claimService.createClaim(claim);

        Claimant expectedClaimant = claimant
                .toBuilder()
                .eligibilityStatus(EligibilityStatus.DUPLICATE)
                .build();
        verify(claimantRepository).eligibleClaimExists(claimant.getNino());
        verify(claimantRepository).save(expectedClaimant);
        verifyZeroInteractions(client);
    }

    @Test
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    /**
     * This is a false positive. PMD can't follow the data flow of `claim` inside the lambda.
     * https://github.com/pmd/pmd/issues/1304
     */
    public void shouldSaveClaimantWhenEligibilityThrowsException() {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        Claim claim = Claim.builder()
                .claimant(claimant)
                .build();
        RuntimeException testException = new RuntimeException("Test exception");
        given(client.checkEligibility(any())).willThrow(testException);

        //when
        RuntimeException thrown = catchThrowableOfType(() -> claimService.createClaim(claim), RuntimeException.class);

        //then
        assertThat(thrown).isEqualTo(testException);
        Claimant expectedClaimant = claimant.toBuilder().eligibilityStatus(EligibilityStatus.ERROR).build();
        verify(claimantRepository).save(expectedClaimant);
        verify(client).checkEligibility(claimant);
        verify(claimantRepository).eligibleClaimExists(claimant.getNino());
    }
}
