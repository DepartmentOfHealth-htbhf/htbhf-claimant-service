package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.exception.EligibilityClientException;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

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
    public void shouldNotSaveClaimantWhenEligibilityThrowsException() {
        //given
        Claimant claimant = aValidClaimant();
        Claim claim = Claim.builder()
                .claimant(claimant)
                .build();
        given(client.checkEligibility(any())).willThrow(new EligibilityClientException(HttpStatus.BAD_REQUEST));

        //when
        claimService.createClaim(claim);

        //then
        //TODO - Add verification to make sure that the status is added to the Claimant before persisting.
        verify(claimantRepository).save(claim.getClaimant());
        verify(client).checkEligibility(claimant);
    }
}
