package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

@Service
@AllArgsConstructor
public class DuplicateClaimChecker {

    private final ClaimRepository claimRepository;

    /**
     * Determines whether a live (new, active, pending or pending expiry) claim exists for the household identifier returned by the DWP or HMRC.
     *
     * @param eligibilityResponse Eligibility response containing household identifiers.
     * @return true if there is already a claim for either of the household identifiers.
     */
    public boolean liveClaimExistsForHousehold(EligibilityResponse eligibilityResponse) {
        boolean dwpClaimExists = liveClaimExistsForDwpHousehold(eligibilityResponse.getDwpHouseholdIdentifier());

        String hmrcHouseholdIdentifier = eligibilityResponse.getHmrcHouseholdIdentifier();
        boolean hmrcClaimExists = hmrcHouseholdIdentifier != null && claimRepository.liveClaimExistsForHmrcHousehold(hmrcHouseholdIdentifier);

        return dwpClaimExists || hmrcClaimExists;
    }

    /**
     * Determines whether a live (new, active, pending or pending expiry) claim exists for the given
     * DWP household identifier.
     *
     * @param dwpHouseholdIdentifier The DWP household identifiers.
     * @return true if there is already a claim for the DWP household identifier.
     */
    public boolean liveClaimExistsForDwpHousehold(String dwpHouseholdIdentifier) {
        return dwpHouseholdIdentifier != null && claimRepository.liveClaimExistsForDwpHousehold(dwpHouseholdIdentifier);
    }
}
