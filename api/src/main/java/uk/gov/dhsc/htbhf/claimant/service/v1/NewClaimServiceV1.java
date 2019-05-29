package uk.gov.dhsc.htbhf.claimant.service.v1;

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
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildWithStatus;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewClaimServiceV1 {

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

    public ClaimResult createClaim(Claimant claimant) {
        try {
            EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateNewClaimant(claimant);
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

    private Claim createAndSaveClaim(Claimant claimant, EligibilityAndEntitlementDecision decision) {
        Claim claim = buildClaim(claimant, decision);
        claimRepository.save(claim);
        log.info("Saved new claimant: {} with status {}", claim.getId(), claim.getEligibilityStatus());
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
