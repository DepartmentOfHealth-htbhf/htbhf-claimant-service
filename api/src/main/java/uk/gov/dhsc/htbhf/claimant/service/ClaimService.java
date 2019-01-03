package uk.gov.dhsc.htbhf.claimant.service;

import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

@Service
public class ClaimService {

    private final ClaimantRepository claimantRepository;

    public ClaimService(ClaimantRepository claimantRepository) {
        this.claimantRepository = claimantRepository;
    }

    public void createClaim(Claim claim) {
        claimantRepository.save(claim.getClaimant());
    }
}
