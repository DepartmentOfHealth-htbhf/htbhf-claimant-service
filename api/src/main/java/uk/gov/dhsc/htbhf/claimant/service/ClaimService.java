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
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.AdditionalPregnancyPaymentMessagePayload;
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

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.ADDITIONAL_PREGNANCY_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.EXPECTED_DELIVERY_DATE;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildWithStatus;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final EligibilityAndEntitlementService eligibilityAndEntitlementService;
    private final EventAuditor eventAuditor;
    private final MessageQueueClient messageQueueClient;

    private static final Map<EligibilityStatus, ClaimStatus> STATUS_MAP = Map.of(
            EligibilityStatus.ELIGIBLE, ClaimStatus.NEW,
            EligibilityStatus.PENDING, ClaimStatus.PENDING,
            EligibilityStatus.NO_MATCH, ClaimStatus.REJECTED,
            EligibilityStatus.ERROR, ClaimStatus.ERROR,
            EligibilityStatus.DUPLICATE, ClaimStatus.REJECTED,
            EligibilityStatus.INELIGIBLE, ClaimStatus.REJECTED
    );

    public ClaimResult createOrUpdateClaim(ClaimRequest claimRequest) {

        try {
            EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateClaimant(claimRequest.getClaimant());
            if (claimExists(decision)) {
                Claim claim = findClaim(decision.getExistingClaimId());

                if (decisionIsNotEligible(decision)) {
                    Claim updatedClaim = handleClaimBecomingIneligible(claim);
                    return createResult(updatedClaim);
                }

                List<String> updatedFields = updateClaim(claim, claimRequest.getClaimant());
                sendAdditionalPaymentMessageIfNewDueDateProvided(claim, updatedFields);
                return createResult(claim, decision.getVoucherEntitlement(), updatedFields);
            }

            Claim claim = createAndSaveClaim(claimRequest, decision);
            if (claim.getClaimStatus() == ClaimStatus.NEW) {
                sendNewCardMessage(claim, decision);
                return createResult(claim, decision.getVoucherEntitlement());
            }

            return createResult(claim);
        } catch (RuntimeException e) {
            handleFailedClaim(claimRequest, e);
            throw e;
        }
    }

    private void sendAdditionalPaymentMessageIfNewDueDateProvided(Claim claim, List<String> updatedFields) {
        if (claim.getClaimant().getExpectedDeliveryDate() != null
                && updatedFields.contains(EXPECTED_DELIVERY_DATE.getFieldName())) {
            AdditionalPregnancyPaymentMessagePayload payload = AdditionalPregnancyPaymentMessagePayload.builder().claimId(claim.getId()).build();
            messageQueueClient.sendMessage(payload, ADDITIONAL_PREGNANCY_PAYMENT);
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

    private boolean decisionIsNotEligible(EligibilityAndEntitlementDecision decision) {
        return decision.getEligibilityStatus() != EligibilityStatus.ELIGIBLE;
    }

    private boolean claimExists(EligibilityAndEntitlementDecision decision) {
        return decision.getExistingClaimId() != null;
    }

    private Claim findClaim(UUID claimId) {
        Optional<Claim> optionalClaim = claimRepository.findById(claimId);
        return optionalClaim.orElseThrow(() -> new IllegalStateException("Unable to find claim with id " + claimId));
    }

    private Claim handleClaimBecomingIneligible(Claim claim) {
        log.info("Updating claim {} to PENDING_EXPIRY", claim.getId());
        LocalDateTime now = LocalDateTime.now();
        claim.setClaimStatus(ClaimStatus.PENDING_EXPIRY);
        claim.setEligibilityStatus(EligibilityStatus.INELIGIBLE);
        claim.setClaimStatusTimestamp(now);
        claim.setEligibilityStatusTimestamp(now);
        Claim updatedClaim = claimRepository.save(claim);
        eventAuditor.auditUpdatedClaim(claim, emptyList());
        return updatedClaim;
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

    private void sendNewCardMessage(Claim claim, EligibilityAndEntitlementDecision decision) {
        NewCardRequestMessagePayload payload = buildNewCardMessagePayload(claim, decision.getVoucherEntitlement(), decision.getDateOfBirthOfChildren());
        messageQueueClient.sendMessage(payload, CREATE_NEW_CARD);
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
