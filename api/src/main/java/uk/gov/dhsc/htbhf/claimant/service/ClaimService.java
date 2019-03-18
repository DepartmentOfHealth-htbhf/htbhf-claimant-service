package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.exception.EligibilityClientException;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimantRepository claimantRepository;
    private final EligibilityClient client;

    /**
     * NOTE: This is only here to keep Checkstyle happy - we'll remove as we resolve the TODOs.
     * TODO - If there is any Exception catch at this point, we need to throw an Exception to make sure that the
     *        failure goes back to the UI.
     * TODO - Add eligibility status to the Claimant and to the database.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void createClaim(Claim claim) {
        Claimant claimant = claim.getClaimant();
        try {
            client.checkEligibility(claimant);
        } catch (EligibilityClientException ece) {
            log.error("Unexpected exception caught trying to determine the Eligibility status from the Eligibility Service", ece);
        } catch (RuntimeException re) {
            log.error("Unexpected exception caught trying to call the Eligibility Service", re);
        }
        claimantRepository.save(claimant);
        log.info("Saved new claimant: {}", claimant.getId());
    }
}
