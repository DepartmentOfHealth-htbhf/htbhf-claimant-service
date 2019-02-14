package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimantRepository claimantRepository;

    public void createClaim(Claim claim) {
        Claimant claimant = claim.getClaimant();
        claimantRepository.save(claimant);
        log.info("Saved new claimant: {}", claimant.getId());
    }
}
