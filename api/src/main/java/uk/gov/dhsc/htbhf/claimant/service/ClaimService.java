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

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Claimant createClaim(Claim claim) {
        Claimant claimant = claim.getClaimant();

        try {
            EligibilityStatus eligibilityStatus;

            if (claimantRepository.eligibleClaimExistsForNino(claimant.getNino())) {
                eligibilityStatus = EligibilityStatus.DUPLICATE;
            } else {
                EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
                claimant.setDwpHouseholdIdentifier(eligibilityResponse.getDwpHouseholdIdentifier());
                claimant.setHmrcHouseholdIdentifier(eligibilityResponse.getHmrcHouseholdIdentifier());
                eligibilityStatus = determineEligibilityStatusForHousehold(eligibilityResponse);
            }

            saveClaimant(claimant, eligibilityStatus);
            return claimant;
        } catch (RuntimeException e) {
            saveClaimant(claimant, EligibilityStatus.ERROR);
            throw e;
        }
    }

    private EligibilityStatus determineEligibilityStatusForHousehold(EligibilityResponse eligibilityResponse) {
        return eligibleClaimExistsForHousehold(eligibilityResponse)
                ? EligibilityStatus.DUPLICATE
                : eligibilityResponse.getEligibilityStatus();
    }

    private boolean eligibleClaimExistsForHousehold(EligibilityResponse eligibilityResponse) {
        String dwpHouseholdIdentifier = eligibilityResponse.getDwpHouseholdIdentifier();

        if (dwpHouseholdIdentifier != null) {
            return claimantRepository.eligibleClaimExistsForDwpHousehold(dwpHouseholdIdentifier);
        }

        String hmrcHouseholdIdentifier = eligibilityResponse.getHmrcHouseholdIdentifier();

        if (hmrcHouseholdIdentifier != null) {
            return claimantRepository.eligibleClaimExistsForHmrcHousehold(hmrcHouseholdIdentifier);
        }

        return false;
    }

    private void saveClaimant(Claimant claimant, EligibilityStatus eligibilityStatus) {
        claimant.setEligibilityStatus(eligibilityStatus);
        claimantRepository.save(claimant);
        log.info("Saved new claimant: {} with status {}", claimant.getId(), claimant.getEligibilityStatus());
    }
}
