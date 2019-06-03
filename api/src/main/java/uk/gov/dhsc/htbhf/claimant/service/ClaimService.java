package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewClaimEvent;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantFields.EXPECTED_DELIVERY_DATE;
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

    public ClaimResult createOrUpdateClaim(Claimant claimant) {
        try {
            EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateClaimant(claimant);
            if (claimExistsAndIsEligible(decision)) {
                Claim claim = findClaim(decision.getExistingClaimId());
                List<String> updatedFields = updateClaim(claim, claimant);
                return createResult(claim, decision.getVoucherEntitlement(), updatedFields);
            }

            Claim claim = createAndSaveClaim(claimant, decision);
            if (claim.getClaimStatus() == ClaimStatus.NEW) {
                sendNewCardMessage(claim, decision);
                return createResult(claim, decision.getVoucherEntitlement());
            }

            return createResult(claim);
        } catch (RuntimeException e) {
            handleFailedClaim(claimant, e);
            throw e;
        }
    }

    private void handleFailedClaim(Claimant claimant, RuntimeException e) {
        Claim claim = buildClaim(claimant, buildWithStatus(EligibilityStatus.ERROR));
        NewClaimEvent newClaimEvent = NewClaimEvent.builder()
                .claimId(claim.getId())
                .claimStatus(claim.getClaimStatus())
                .eligibilityStatus(claim.getEligibilityStatus())
                .build();
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
        if (!Objects.equals(claim.getClaimant().getExpectedDeliveryDate(), claimant.getExpectedDeliveryDate())) {
            claim.getClaimant().setExpectedDeliveryDate(claimant.getExpectedDeliveryDate());
            return singletonList(EXPECTED_DELIVERY_DATE.getFieldName());
        }

        return emptyList();
    }

    private Claim createAndSaveClaim(Claimant claimant, EligibilityAndEntitlementDecision decision) {
        Claim claim = buildClaim(claimant, decision);
        claimRepository.save(claim);
        log.info("Saved new claim: {} with status {}", claim.getId(), claim.getEligibilityStatus());
        eventAuditor.auditNewClaim(claim);
        return claim;
    }

    private void sendNewCardMessage(Claim claim, EligibilityAndEntitlementDecision decision) {
        NewCardRequestMessagePayload payload = buildNewCardMessagePayload(claim, decision.getVoucherEntitlement(), decision.getDateOfBirthOfChildren());
        messageQueueDAO.sendMessage(payload, CREATE_NEW_CARD);
    }

    private Claim buildClaim(Claimant claimant, EligibilityAndEntitlementDecision decision) {
        ClaimStatus claimStatus = STATUS_MAP.get(decision.getEligibilityStatus());
        LocalDateTime currentDateTime = LocalDateTime.now();
        return Claim.builder()
                .dwpHouseholdIdentifier(decision.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(decision.getHmrcHouseholdIdentifier())
                .eligibilityStatus(decision.getEligibilityStatus())
                .eligibilityStatusTimestamp(currentDateTime)
                .claimStatus(claimStatus)
                .claimStatusTimestamp(currentDateTime)
                .claimant(claimant)
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
