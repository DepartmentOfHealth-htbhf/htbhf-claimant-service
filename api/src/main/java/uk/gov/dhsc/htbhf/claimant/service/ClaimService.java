package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimDTOToClaimConverter;
import uk.gov.dhsc.htbhf.claimant.entitlement.EntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
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
    private final ClaimDTOToClaimConverter converter;
    private final EntitlementCalculator entitlementCalculator;

    private static final Map<EligibilityStatus, ClaimStatus> STATUS_MAP = Map.of(
            EligibilityStatus.ELIGIBLE, ClaimStatus.NEW,
            EligibilityStatus.PENDING, ClaimStatus.PENDING,
            EligibilityStatus.NO_MATCH, ClaimStatus.REJECTED,
            EligibilityStatus.ERROR, ClaimStatus.ERROR,
            EligibilityStatus.DUPLICATE, ClaimStatus.REJECTED,
            EligibilityStatus.INELIGIBLE, ClaimStatus.REJECTED
    );

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Claim createClaim(ClaimDTO claimDTO) {
        Claim claim = converter.convert(claimDTO);
        Claimant claimant = claim.getClaimant();

        try {
            EligibilityResponse eligibilityResponse = determineEligibilityStatus(claimant);
            EligibilityStatus eligibilityStatus = eligibilityResponse.getEligibilityStatus();
            ClaimStatus claimStatus = STATUS_MAP.get(eligibilityStatus);
            updateClaimFromEligibilityResponse(claim, eligibilityResponse);
            saveClaimant(claimant, claimStatus, eligibilityStatus);
            return claim;
        } catch (RuntimeException e) {
            saveClaimant(claimant, ClaimStatus.ERROR, EligibilityStatus.ERROR);
            throw e;
        }
    }

    private EligibilityResponse determineEligibilityStatus(Claimant claimant) {
        if (claimantRepository.eligibleClaimExistsForNino(claimant.getNino())) {
            return EligibilityResponse.builder()
                    .eligibilityStatus(EligibilityStatus.DUPLICATE)
                    .build();
        } else {
            EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
            EligibilityStatus eligibilityStatus = eligibilityStatusCalculator.determineEligibilityStatus(eligibilityResponse);
            return eligibilityResponse.toBuilder()
                    .eligibilityStatus(eligibilityStatus)
                    .build();
        }
    }

    private void updateClaimFromEligibilityResponse(Claim claim, EligibilityResponse eligibilityResponse) {
        Claimant claimant = claim.getClaimant();
        claimant.setDwpHouseholdIdentifier(eligibilityResponse.getDwpHouseholdIdentifier());
        claimant.setHmrcHouseholdIdentifier(eligibilityResponse.getHmrcHouseholdIdentifier());

        VoucherEntitlement entitlement = entitlementCalculator.calculateVoucherEntitlement(claimant, eligibilityResponse);
        claim.setVoucherEntitlement(entitlement);
    }

    private void saveClaimant(Claimant claimant, ClaimStatus claimStatus, EligibilityStatus eligibilityStatus) {
        claimant.setClaimStatus(claimStatus);
        claimant.setEligibilityStatus(eligibilityStatus);
        claimantRepository.save(claimant);
        log.info("Saved new claimant: {} with status {}", claimant.getId(), claimant.getEligibilityStatus());
    }
}
