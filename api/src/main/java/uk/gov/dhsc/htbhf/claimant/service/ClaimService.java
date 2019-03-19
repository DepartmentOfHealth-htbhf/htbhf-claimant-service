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
    private final EligibilityClient client;

    /**
     * NOTE: This is only here to keep Checkstyle happy - we'll remove as we resolve the TODOs.
     * TODO - Add eligibility status to the Claimant and to the database. If EligibilityClientException caught, set
     *        status to error on claimant. Ensure status is also logged.
     */
    public void createClaim(Claim claim) {
        Claimant claimant = claim.getClaimant();
        try {
            client.checkEligibility(claimant);
        } finally {
            claimantRepository.save(claimant);
            log.info("Saved new claimant: {}", claimant.getId());
        }
    }
}
