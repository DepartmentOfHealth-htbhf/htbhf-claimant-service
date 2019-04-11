package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

@Service
@AllArgsConstructor
public class EligibilityStatusCalculator {

    private final ClaimantRepository claimantRepository;

    /**
     * Determines the eligibility status for a given {@link EligibilityResponse}.
     * The status is DUPLICATE if a live claim exists for the given dwp or hmrc household identifier,
     * otherwise returns the eligibility status from the given eligibility response.
     * @param eligibilityResponse Eligibility response containing household identifiers.
     * @return the eligibility status
     */
    public EligibilityStatus determineEligibilityStatus(EligibilityResponse eligibilityResponse) {
        return eligibleClaimExistsForHousehold(eligibilityResponse)
                ? EligibilityStatus.DUPLICATE
                : eligibilityResponse.getEligibilityStatus();
    }

    private boolean eligibleClaimExistsForHousehold(EligibilityResponse eligibilityResponse) {
        String dwpHouseholdIdentifier = eligibilityResponse.getDwpHouseholdIdentifier();
        boolean dwpClaimExists = dwpHouseholdIdentifier != null && claimantRepository.liveClaimExistsForDwpHousehold(dwpHouseholdIdentifier);

        String hmrcHouseholdIdentifier = eligibilityResponse.getHmrcHouseholdIdentifier();
        boolean hmrcClaimExists = hmrcHouseholdIdentifier != null && claimantRepository.liveClaimExistsForHmrcHousehold(hmrcHouseholdIdentifier);

        return dwpClaimExists || hmrcClaimExists;
    }
}
