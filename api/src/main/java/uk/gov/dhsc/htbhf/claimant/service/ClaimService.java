package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimantRepository claimantRepository;
    private final EligibilityClient client;
    private final EligibilityStatusCalculator eligibilityStatusCalculator;

    private static final Map<EligibilityStatus, ClaimStatus> STATUS_MAP = Map.of(
            EligibilityStatus.ELIGIBLE, ClaimStatus.NEW,
            EligibilityStatus.PENDING, ClaimStatus.PENDING,
            EligibilityStatus.NO_MATCH, ClaimStatus.REJECTED,
            EligibilityStatus.ERROR, ClaimStatus.ERROR,
            EligibilityStatus.DUPLICATE, ClaimStatus.REJECTED,
            EligibilityStatus.INELIGIBLE, ClaimStatus.REJECTED
    );

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Claimant createClaim(Claim claim) {
        Claimant claimant = claim.getClaimant();

        try {
            EligibilityStatus eligibilityStatus = determineEligibilityStatus(claimant);
            ClaimStatus claimStatus = STATUS_MAP.get(eligibilityStatus);
            saveClaimant(claimant, claimStatus, eligibilityStatus);
            return claimant;
        } catch (RuntimeException e) {
            saveClaimant(claimant, ClaimStatus.ERROR, EligibilityStatus.ERROR);
            throw e;
        }
    }

    private EligibilityStatus determineEligibilityStatus(Claimant claimant) {
        if (claimantRepository.eligibleClaimExistsForNino(claimant.getNino())) {
            return EligibilityStatus.DUPLICATE;
        } else {
            EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
            claimant.setDwpHouseholdIdentifier(eligibilityResponse.getDwpHouseholdIdentifier());
            claimant.setHmrcHouseholdIdentifier(eligibilityResponse.getHmrcHouseholdIdentifier());
            return eligibilityStatusCalculator.determineEligibilityStatus(eligibilityResponse);
        }
    }

    private void saveClaimant(Claimant claimant, ClaimStatus claimStatus, EligibilityStatus eligibilityStatus) {
        claimant.setClaimStatus(claimStatus);
        claimant.setEligibilityStatus(eligibilityStatus);
        claimantRepository.save(claimant);
        log.info("Saved new claimant: {} with status {}", claimant.getId(), claimant.getEligibilityStatus());
    }
}
