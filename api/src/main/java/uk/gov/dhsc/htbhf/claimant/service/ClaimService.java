package uk.gov.dhsc.htbhf.claimant.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewClaimEvent;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.EXPECTED_DELIVERY_DATE;
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

    public ClaimResult createOrUpdateClaim(ClaimRequest claimRequest) {
        ClaimResult claimResult = processClaimRequest(claimRequest);
        claimMessageSender.sendReportClaimMessage(claimResult.getClaim());
        return claimResult;
    }

    private ClaimResult processClaimRequest(ClaimRequest claimRequest) {
        try {
            EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateClaimant(claimRequest.getClaimant());
            if (decision.claimExistsAndIsEligible()) {
                Claim claim = claimRepository.findClaim(decision.getExistingClaimId());
                List<String> updatedFields = updateClaim(claim, claimRequest.getClaimant());
                sendAdditionalPaymentMessageIfNewDueDateProvided(claim, updatedFields);
                VoucherEntitlement weeklyEntitlement = decision.getVoucherEntitlement().getFirstVoucherEntitlementForCycle();
                return ClaimResult.withEntitlementAndUpdatedFields(claim, weeklyEntitlement, updatedFields);
            }

            Claim claim = createAndSaveClaim(claimRequest, decision);
            if (claim.getClaimStatus() == ClaimStatus.NEW) {
                claimMessageSender.sendNewCardMessage(claim, decision);
                VoucherEntitlement weeklyEntitlement = decision.getVoucherEntitlement().getFirstVoucherEntitlementForCycle();
                return ClaimResult.withEntitlement(claim, weeklyEntitlement);
            }

            return ClaimResult.withNoEntitlement(claim);
        } catch (RuntimeException e) {
            handleFailedClaim(claimRequest, e);
            throw e;
        }
    }

    private void sendAdditionalPaymentMessageIfNewDueDateProvided(Claim claim, List<String> updatedFields) {
        if (claim.getClaimant().getExpectedDeliveryDate() != null && updatedFields.contains(EXPECTED_DELIVERY_DATE.getFieldName())) {
            claimMessageSender.sendAdditionalPaymentMessage(claim);
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

    private List<String> updateClaim(Claim claim, Claimant claimant) {
        List<String> updatedFields = updateClaimantFields(claim, claimant);
        log.info("Updating claim: {},  fields updated {}", claim.getId(), updatedFields);
        claimRepository.save(claim);
        eventAuditor.auditUpdatedClaim(claim, updatedFields);
        return updatedFields;
    }

    private List<String> updateClaimantFields(Claim claim, Claimant claimant) {
        Claimant originalClaimant = claim.getClaimant();
        List<String> updatedFields = new ArrayList<>();
        for (UpdatableClaimantField field : UpdatableClaimantField.values()) {
            if (field.valueIsDifferent(originalClaimant, claimant)) {
                field.updateOriginal(originalClaimant, claimant);
                updatedFields.add(field.getFieldName());
            }
        }
        return updatedFields;
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
                .build();
    }

}
