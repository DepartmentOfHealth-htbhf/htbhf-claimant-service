package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.DataflowAnomalyAnalysis") // PMD does not like reassignment of `eligibilityStatus`
public class ClaimService {

    private final ClaimantRepository claimantRepository;
    private final EligibilityClient client;
    private final Map<EligibilityStatus, HttpStatus> statusMap = Map.of(
      EligibilityStatus.ELIGIBLE, HttpStatus.CREATED,
      EligibilityStatus.DUPLICATE, HttpStatus.OK
    );

    public Claimant createClaim(Claim claim) {
        Claimant claimant = claim.getClaimant();
        EligibilityStatus eligibilityStatus = EligibilityStatus.ERROR;

        try {
            if (claimantRepository.claimantExists(claimant.getNino())) {
                eligibilityStatus = EligibilityStatus.DUPLICATE;
            } else {
                EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
                eligibilityStatus = eligibilityResponse.getEligibilityStatus();
                claimant.setHouseholdIdentifier(eligibilityResponse.getHouseholdIdentifier());
            }
        } finally {
            claimant.setEligibilityStatus(eligibilityStatus);
            claimantRepository.save(claimant);
            log.info("Saved new claimant: {} with status {}", claimant.getId(), claimant.getEligibilityStatus());
            return claimant;
        }
    }

    public ResponseEntity createResponseFromClaimant(Claimant claimant) {
        EligibilityStatus eligibilityStatus = claimant.getEligibilityStatus();
        HttpStatus statusCode = statusMap.get(eligibilityStatus);
        ClaimResponse body = ClaimResponse.builder().eligibilityStatus(eligibilityStatus).build();
        return new ResponseEntity(body, statusCode);
    }
}
