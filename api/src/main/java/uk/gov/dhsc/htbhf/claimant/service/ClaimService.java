package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimDTOToClaimConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimantRepository claimantRepository;
    private final EligibilityClient client;
    private final EligibilityStatusCalculator eligibilityStatusCalculator;
    private final ClaimDTOToClaimConverter converter;

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Claimant createClaim(ClaimDTO claimDTO) {
        Claim claim = converter.convert(claimDTO);

        Claimant claimant = claim.getClaimant();

        try {
            EligibilityStatus eligibilityStatus;

            if (claimantRepository.eligibleClaimExistsForNino(claimant.getNino())) {
                eligibilityStatus = EligibilityStatus.DUPLICATE;
            } else {
                EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
                claimant.setDwpHouseholdIdentifier(eligibilityResponse.getDwpHouseholdIdentifier());
                claimant.setHmrcHouseholdIdentifier(eligibilityResponse.getHmrcHouseholdIdentifier());
                eligibilityStatus = eligibilityStatusCalculator.determineEligibilityStatus(eligibilityResponse);
            }

            saveClaimant(claimant, eligibilityStatus);
            return claimant;
        } catch (RuntimeException e) {
            saveClaimant(claimant, EligibilityStatus.ERROR);
            throw e;
        }
    }

    private void saveClaimant(Claimant claimant, EligibilityStatus eligibilityStatus) {
        claimant.setEligibilityStatus(eligibilityStatus);
        claimantRepository.save(claimant);
        log.info("Saved new claimant: {} with status {}", claimant.getId(), claimant.getEligibilityStatus());
    }
}
