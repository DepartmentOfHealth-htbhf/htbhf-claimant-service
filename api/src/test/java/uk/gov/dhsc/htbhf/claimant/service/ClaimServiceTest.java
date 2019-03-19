package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
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
    public void shouldSaveClaimant() {
        //given
        Claimant claimant = aValidClaimant();
        Claim claim = Claim.builder()
                .claimant(claimant)
                .build();
        given(client.checkEligibility(any())).willReturn(anEligibilityResponse());

        //when
        claimService.createClaim(claim);

        //then
        verify(claimantRepository).save(claim.getClaimant());
        verify(client).checkEligibility(claimant);
    }

    @Test
    public void shouldSaveClaimantWhenEligibilityThrowsException() {
        //given
        Claimant claimant = aValidClaimant();
        Claim claim = Claim.builder()
                .claimant(claimant)
                .build();
        RuntimeException testException = new RuntimeException("Test exception");
        given(client.checkEligibility(any())).willThrow(testException);

        //when
        RuntimeException thrown = catchThrowableOfType(() -> claimService.createClaim(claim), RuntimeException.class);

        //then
        //TODO - Add verification to make sure that the status is added to the Claimant before persisting.
        assertThat(thrown).isEqualTo(testException);
        verify(claimantRepository).save(claim.getClaimant());
        verify(client).checkEligibility(claimant);
    }
}
