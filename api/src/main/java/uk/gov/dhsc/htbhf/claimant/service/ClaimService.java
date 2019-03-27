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
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class ClaimService {

    private final ClaimantRepository claimantRepository;
    private final EligibilityClient client;

    public Claimant createClaim(Claim claim) {
        Claimant claimant = claim.getClaimant();

        try {
            EligibilityStatus eligibilityStatus;

            if (claimantDoesNotExist(claimant.getNino())) {
                EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
                eligibilityStatus = eligibilityResponse.getEligibilityStatus();
                claimant.setHouseholdIdentifier(eligibilityResponse.getHouseholdIdentifier());
            } else {
                eligibilityStatus = EligibilityStatus.DUPLICATE;
            }

            saveClaimant(claimant, eligibilityStatus);
            return claimant;
        } catch (Exception e) {
            saveClaimant(claimant, EligibilityStatus.ERROR);
            throw e;
        }
    }

    private Boolean claimantDoesNotExist(String nino) {
        return !claimantRepository.claimantExists(nino);
    }

    private void saveClaimant(Claimant claimant, EligibilityStatus eligibilityStatus) {
        claimant.setEligibilityStatus(eligibilityStatus);
        claimantRepository.save(claimant);
        log.info("Saved new claimant: {} with status {}", claimant.getId(), claimant.getEligibilityStatus());
    }
}
