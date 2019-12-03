package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

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
        boolean hmrcClaimExists = liveClaimExistsForHmrcHousehold(eligibilityResponse.getHmrcHouseholdIdentifier());

        return dwpClaimExists || hmrcClaimExists;
    }

    /**
     * Determines whether a live (new, active, pending or pending expiry) claim exists for the household identifier returned by the DWP or HMRC.
     *
     * @param identityAndEligibilityResponse Identity and Eligibility response containing household identifiers.
     * @return true if there is already a claim for either of the household identifiers.
     */
    public boolean liveClaimExistsForHousehold(CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse) {
        boolean dwpClaimExists = liveClaimExistsForDwpHousehold(identityAndEligibilityResponse.getDwpHouseholdIdentifier());
        boolean hmrcClaimExists = liveClaimExistsForHmrcHousehold(identityAndEligibilityResponse.getHmrcHouseholdIdentifier());

        return dwpClaimExists || hmrcClaimExists;
    }

    /**
     * Determines whether a live (new, active, pending or pending expiry) claim exists for the given
     * DWP household identifier.
     *
     * @param dwpHouseholdIdentifier The DWP household identifier.
     * @return true if there is already a claim for the DWP household identifier.
     */
    public boolean liveClaimExistsForDwpHousehold(String dwpHouseholdIdentifier) {
        return dwpHouseholdIdentifier != null && claimRepository.liveClaimExistsForDwpHousehold(dwpHouseholdIdentifier);
    }

    /**
     * Determines whether a live (new, active, pending or pending expiry) claim exists for the given
     * HMRC household identifier.
     *
     * @param hmrcHouseholdIdentifier The HMRC household identifier.
     * @return true if there is already a claim for the HMRC household identifier.
     */
    public boolean liveClaimExistsForHmrcHousehold(String hmrcHouseholdIdentifier) {
        return hmrcHouseholdIdentifier != null && claimRepository.liveClaimExistsForHmrcHousehold(hmrcHouseholdIdentifier);
    }
}
