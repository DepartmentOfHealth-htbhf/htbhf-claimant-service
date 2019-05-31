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
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

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
                Claim claim = findAndUpdateClaim(claimant, decision);
                return createResult(claim, decision.getVoucherEntitlement());
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

    private Claim findAndUpdateClaim(Claimant claimant, EligibilityAndEntitlementDecision decision) {
        Optional<Claim> optionalClaim = claimRepository.findById(decision.getExistingClaimId());
        Claim claim = optionalClaim.orElseThrow(() -> new IllegalStateException("Unable to find claim with id " + decision.getExistingClaimId()));
        List<String> updatedFields = updateClaim(claim, claimant);
        log.info("Updated claim: {},  fields updated {}", claim.getId(), updatedFields);
        eventAuditor.auditUpdatedClaim(claim, updatedFields);
        return claim;
    }

    private List<String> updateClaim(Claim claim, Claimant claimant) {
        if (!Objects.equals(claim.getClaimant().getExpectedDeliveryDate(), claimant.getExpectedDeliveryDate())) {
            claim.getClaimant().setExpectedDeliveryDate(claimant.getExpectedDeliveryDate());
            claimRepository.save(claim);
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
        VoucherEntitlement firstVoucherEntitlement = voucherEntitlement.getFirstVoucherEntitlementForCycle();

        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(firstVoucherEntitlement))
                .build();
    }

}
