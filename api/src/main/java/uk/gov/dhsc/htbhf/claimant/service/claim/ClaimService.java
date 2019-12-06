package uk.gov.dhsc.htbhf.claimant.service.claim;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewClaimEvent;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDateTime;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildWithStatus;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final EligibilityAndEntitlementService eligibilityAndEntitlementService;
    private final EventAuditor eventAuditor;
    private final ClaimMessageSender claimMessageSender;

    private static final Map<EligibilityStatus, ClaimStatus> STATUS_MAP = Map.of(
            EligibilityStatus.ELIGIBLE, ClaimStatus.NEW,
            EligibilityStatus.PENDING, ClaimStatus.PENDING,
            EligibilityStatus.NO_MATCH, ClaimStatus.REJECTED,
            EligibilityStatus.ERROR, ClaimStatus.ERROR,
            EligibilityStatus.DUPLICATE, ClaimStatus.REJECTED,
            EligibilityStatus.INELIGIBLE, ClaimStatus.REJECTED
    );

    // TODO HTBHF-2682 Rename this method (and others) to createClaim.
    public ClaimResult createOrUpdateClaim(ClaimRequest claimRequest) {
        try {
            EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateNewClaimant(claimRequest.getClaimant());
            if (decision.getEligibilityStatus() == EligibilityStatus.DUPLICATE) {
                Claim claim = createAndSaveClaim(claimRequest, decision);
                claimMessageSender.sendReportClaimMessage(claim, emptyList(), ClaimAction.REJECTED);
                return ClaimResult.withNoEntitlement(claim);
            }

            Claim claim = createAndSaveClaim(claimRequest, decision);
            if (claim.getClaimStatus() == ClaimStatus.NEW) {
                claimMessageSender.sendInstantSuccessEmailMessage(claim, decision);
                claimMessageSender.sendNewCardMessage(claim, decision);
                VoucherEntitlement weeklyEntitlement = decision.getVoucherEntitlement().getFirstVoucherEntitlementForCycle();
                claimMessageSender.sendReportClaimMessage(claim, decision.getDateOfBirthOfChildren(), ClaimAction.NEW);
                return ClaimResult.withEntitlement(claim, weeklyEntitlement, decision.getIdentityAndEligibilityResponse());
            }

            claimMessageSender.sendReportClaimMessage(claim, decision.getDateOfBirthOfChildren(), ClaimAction.REJECTED);
            return ClaimResult.withNoEntitlement(claim, decision.getIdentityAndEligibilityResponse());
        } catch (RuntimeException e) {
            handleFailedClaim(claimRequest, e);
            throw e;
        }
    }

    private void handleFailedClaim(ClaimRequest claimRequest, RuntimeException e) {
        EligibilityAndEntitlementDecision decision = buildWithStatus(EligibilityStatus.ERROR);
        Claim claim = buildClaim(claimRequest, decision);
        NewClaimEvent newClaimEvent = new NewClaimEvent(claim);
        FailureEvent failureEvent = FailureEvent.builder()
                .failureDescription("Unable to create (or update) claim")
                .failedEvent(newClaimEvent)
                .exception(e)
                .build();
        eventAuditor.auditFailedEvent(failureEvent);
        claimRepository.save(claim);
    }

    private Claim createAndSaveClaim(ClaimRequest claimRequest, EligibilityAndEntitlementDecision decision) {
        Claim claim = buildClaim(claimRequest, decision);
        log.info("Saving new claim: {} with status {}", claim.getId(), claim.getEligibilityStatus());
        claimRepository.save(claim);
        eventAuditor.auditNewClaim(claim);
        return claim;
    }

    @SuppressFBWarnings(value = "SECMD5",
            justification = "Using a hash of the device fingerprint to identify multiple claims from the same device, not for encryption")
    private Claim buildClaim(ClaimRequest claimRequest, EligibilityAndEntitlementDecision decision) {
        ClaimStatus claimStatus = STATUS_MAP.get(decision.getEligibilityStatus());
        LocalDateTime currentDateTime = LocalDateTime.now();
        Map<String, Object> deviceFingerprint = claimRequest.getDeviceFingerprint();
        String fingerprintHash = isEmpty(deviceFingerprint) ? null : DigestUtils.md5Hex(deviceFingerprint.toString());
        return Claim.builder()
                .dwpHouseholdIdentifier(decision.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(decision.getHmrcHouseholdIdentifier())
                .eligibilityStatus(decision.getEligibilityStatus())
                .eligibilityStatusTimestamp(currentDateTime)
                .claimStatus(claimStatus)
                .claimStatusTimestamp(currentDateTime)
                .claimant(claimRequest.getClaimant())
                .deviceFingerprint(deviceFingerprint)
                .deviceFingerprintHash(fingerprintHash)
                .webUIVersion(claimRequest.getWebUIVersion())
                .initialIdentityAndEligibilityResponse(decision.getIdentityAndEligibilityResponse())
                .build();
    }

}
