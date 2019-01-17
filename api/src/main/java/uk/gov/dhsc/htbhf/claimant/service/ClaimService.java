package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimantRepository claimantRepository;

    public void createClaim(Claim claim) {
        claimantRepository.save(claim.getClaimant());
    }
}
