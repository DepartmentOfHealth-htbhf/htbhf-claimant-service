package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

@Service
@AllArgsConstructor
public class EligibilityService {

    private final EligibilityClient client;
    private final EligibilityStatusCalculator eligibilityStatusCalculator;
    private final ClaimRepository claimRepository;

    public EligibilityResponse determineEligibility(Claimant claimant) {
        if (claimRepository.liveClaimExistsForNino(claimant.getNino())) {
            return EligibilityResponse.withStatus(EligibilityStatus.DUPLICATE);
        }
        EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
        EligibilityStatus eligibilityStatus = eligibilityStatusCalculator.determineEligibilityStatus(eligibilityResponse);
        return eligibilityResponse.toBuilder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }
}
