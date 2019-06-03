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
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantFields;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

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
// TODO DW rename to ClaimService in own PR
public class NewClaimService {

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
            EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateNewClaimant(claimant);
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
            createAndSaveClaim(claimant, buildWithStatus(EligibilityStatus.ERROR));
            throw e;
        }
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
        for (UpdatableClaimantFields field : UpdatableClaimantFields.values()) {
            if (field.valueIsDifferent(originalClaimant, claimant)) {
                field.updateOriginal(originalClaimant, claimant);
                updatedFields.add(field.getFieldName());
            }
        }
        return updatedFields;
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
        boolean claimUpdated = !isEmpty(updatedFields);

        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(firstVoucherEntitlement))
                .updatedFields(updatedFields)
                .claimUpdated(claimUpdated)
                .build();
    }

}
