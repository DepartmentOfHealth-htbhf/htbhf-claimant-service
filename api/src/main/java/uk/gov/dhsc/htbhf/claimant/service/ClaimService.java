package uk.gov.dhsc.htbhf.claimant.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewClaimEvent;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildWithStatus;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final EligibilityAndEntitlementService eligibilityAndEntitlementService;
    private final EventAuditor eventAuditor;
    private final MessageQueueDAO messageQueueDAO;

    private static final Map<EligibilityStatus, ClaimStatus> STATUS_MAP = Map.of(
            EligibilityStatus.ELIGIBLE, ClaimStatus.NEW,
            EligibilityStatus.PENDING, ClaimStatus.PENDING,
            EligibilityStatus.NO_MATCH, ClaimStatus.REJECTED,
            EligibilityStatus.ERROR, ClaimStatus.ERROR,
            EligibilityStatus.DUPLICATE, ClaimStatus.REJECTED,
            EligibilityStatus.INELIGIBLE, ClaimStatus.REJECTED
    );

    public ClaimResult createOrUpdateClaim(Claimant claimant, Map<String, Object> deviceFingerprint) {
        try {
            EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateClaimant(claimant);
            if (claimExistsAndIsEligible(decision)) {
                Claim claim = findClaim(decision.getExistingClaimId());
                List<String> updatedFields = updateClaim(claim, claimant);
                return createResult(claim, decision.getVoucherEntitlement(), updatedFields);
            }

            Claim claim = createAndSaveClaim(claimant, decision, deviceFingerprint);
            if (claim.getClaimStatus() == ClaimStatus.NEW) {
                sendNewCardMessage(claim, decision);
                return createResult(claim, decision.getVoucherEntitlement());
            }

            return createResult(claim);
        } catch (RuntimeException e) {
            handleFailedClaim(claimant, deviceFingerprint, e);
            throw e;
        }
    }

    private void handleFailedClaim(Claimant claimant, Map<String, Object> deviceFingerprint, RuntimeException e) {
        EligibilityAndEntitlementDecision decision = buildWithStatus(EligibilityStatus.ERROR);
        Claim claim = buildClaim(claimant, decision, deviceFingerprint);
        NewClaimEvent newClaimEvent = new NewClaimEvent(claim);
        FailureEvent failureEvent = FailureEvent.builder()
                .failureDescription("Unable to create (or update) claim")
                .failedEvent(newClaimEvent)
                .exception(e)
                .build();
        eventAuditor.auditFailedEvent(failureEvent);
        claimRepository.save(claim);
    }

    private boolean claimExistsAndIsEligible(EligibilityAndEntitlementDecision decision) {
        return decision.getExistingClaimId() != null && decision.getEligibilityStatus() == EligibilityStatus.ELIGIBLE;
    }

    private Claim findClaim(UUID claimId) {
        Optional<Claim> optionalClaim = claimRepository.findById(claimId);
        return optionalClaim.orElseThrow(() -> new IllegalStateException("Unable to find claim with id " + claimId));
    }

    private List<String> updateClaim(Claim claim, Claimant claimant) {
        List<String> updatedFields = updateClaimantFields(claim, claimant);
        claimRepository.save(claim);
        log.info("Updated claim: {},  fields updated {}", claim.getId(), updatedFields);
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

    private Claim createAndSaveClaim(Claimant claimant, EligibilityAndEntitlementDecision decision, Map<String, Object> deviceFingerprint) {
        Claim claim = buildClaim(claimant, decision, deviceFingerprint);
        claimRepository.save(claim);
        log.info("Saved new claim: {} with status {}", claim.getId(), claim.getEligibilityStatus());
        eventAuditor.auditNewClaim(claim);
        return claim;
    }

    private void sendNewCardMessage(Claim claim, EligibilityAndEntitlementDecision decision) {
        NewCardRequestMessagePayload payload = buildNewCardMessagePayload(claim, decision.getVoucherEntitlement(), decision.getDateOfBirthOfChildren());
        messageQueueDAO.sendMessage(payload, CREATE_NEW_CARD);
    }

    @SuppressFBWarnings(value = "SECMD5",
            justification = "Using a hash of the device fingerprint to identify multiple claims from the same device, not for encryption")
    private Claim buildClaim(Claimant claimant, EligibilityAndEntitlementDecision decision, Map<String, Object> deviceFingerprint) {
        ClaimStatus claimStatus = STATUS_MAP.get(decision.getEligibilityStatus());
        LocalDateTime currentDateTime = LocalDateTime.now();
        String fingerprintHash = isEmpty(deviceFingerprint) ? null : DigestUtils.md5Hex(deviceFingerprint.toString());
        return Claim.builder()
                .dwpHouseholdIdentifier(decision.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(decision.getHmrcHouseholdIdentifier())
                .eligibilityStatus(decision.getEligibilityStatus())
                .eligibilityStatusTimestamp(currentDateTime)
                .claimStatus(claimStatus)
                .claimStatusTimestamp(currentDateTime)
                .claimant(claimant)
                .deviceFingerprint(deviceFingerprint)
                .deviceFingerprintHash(fingerprintHash)
                .build();
    }

    private ClaimResult createResult(Claim claim) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.empty())
                .build();
    }

    private ClaimResult createResult(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement) {
        return createResult(claim, voucherEntitlement, null);
    }

    private ClaimResult createResult(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement, List<String> updatedFields) {
        VoucherEntitlement firstVoucherEntitlement = voucherEntitlement.getFirstVoucherEntitlementForCycle();
        boolean claimUpdated = updatedFields != null;

        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(firstVoucherEntitlement))
                .updatedFields(updatedFields)
                .claimUpdated(claimUpdated)
                .build();
    }

}
