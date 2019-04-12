package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.EntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewClaimService {

    private final ClaimRepository claimRepository;
    private final EligibilityClient client;
    private final EligibilityStatusCalculator eligibilityStatusCalculator;
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
    public ClaimResult createClaim(Claimant claimant) {
        try {
            EligibilityResponse eligibilityResponse = determineEligibility(claimant);
            Claim claim = createAndSaveClaim(claimant, eligibilityResponse);
            return createResult(claim, eligibilityResponse);
        } catch (RuntimeException e) {
            createAndSaveClaim(claimant, eligibilityResponseWithStatus(EligibilityStatus.ERROR));
            throw e;
        }
    }

    private EligibilityResponse determineEligibility(Claimant claimant) {
        if (claimRepository.liveClaimExistsForNino(claimant.getNino())) {
            return eligibilityResponseWithStatus(EligibilityStatus.DUPLICATE);
        } else {
            EligibilityResponse eligibilityResponse = client.checkEligibility(claimant);
            EligibilityStatus eligibilityStatus = eligibilityStatusCalculator.determineEligibilityStatus(eligibilityResponse);
            return eligibilityResponse.toBuilder()
                    .eligibilityStatus(eligibilityStatus)
                    .build();
        }
    }

    private EligibilityResponse eligibilityResponseWithStatus(EligibilityStatus status) {
        return EligibilityResponse.builder()
                .eligibilityStatus(status)
                .build();
    }

    private Claim createAndSaveClaim(Claimant claimant, EligibilityResponse eligibilityResponse) {
        EligibilityStatus eligibilityStatus = eligibilityResponse.getEligibilityStatus();
        ClaimStatus claimStatus = STATUS_MAP.get(eligibilityStatus);
        Claim claim = Claim.builder()
                .dwpHouseholdIdentifier(eligibilityResponse.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(eligibilityResponse.getHmrcHouseholdIdentifier())
                .eligibilityStatus(eligibilityStatus)
                .eligibilityStatusTimestamp(LocalDateTime.now())
                .claimStatus(claimStatus)
                .claimStatusTimestamp(LocalDateTime.now())
                .claimant(claimant)
                .build();

        claimRepository.save(claim);
        log.info("Saved new claimant: {} with status {}", claim.getId(), claim.getEligibilityStatus());
        return claim;
    }

    private ClaimResult createResult(Claim claim, EligibilityResponse eligibilityResponse) {
        VoucherEntitlement entitlement = entitlementCalculator.calculateVoucherEntitlement(claim.getClaimant(), eligibilityResponse);
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(entitlement)
                .build();
    }
}