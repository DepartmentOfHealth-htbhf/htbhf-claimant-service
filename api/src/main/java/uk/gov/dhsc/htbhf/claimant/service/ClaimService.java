package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimantRepository claimantRepository;
    private final EligibilityClient client;

    public void createClaim(Claim claim) {
        Claimant claimant = claim.getClaimant();

        try {
            EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
            claimant.setEligibilityStatus(eligibilityResponse.getEligibilityStatus());
        } finally {
            if (claimant.getEligibilityStatus() == null) {
                claimant.setEligibilityStatus(EligibilityStatus.ERROR);
            }
            claimantRepository.save(claimant);
            log.info("Saved new claimant: {}", claimant.getId());
        }
    }
}
