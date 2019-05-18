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
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlement;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.min;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlement.buildWithStatus;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewClaimService {

    private final ClaimRepository claimRepository;
    private final EligibilityService eligibilityService;
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
            EligibilityAndEntitlement eligibility = eligibilityService.determineEligibilityForNewClaimant(claimant);
            Claim claim = createAndSaveClaim(claimant, eligibility);
            if (claim.getClaimStatus() == ClaimStatus.NEW) {
                sendNewCardMessage(claim, eligibility.getVoucherEntitlement());
                return createResult(claim, eligibility.getVoucherEntitlement());
            }
            return createResult(claim);
        } catch (RuntimeException e) {
            createAndSaveClaim(claimant, buildWithStatus(EligibilityStatus.ERROR));
            throw e;
        }
    }

    private Claim createAndSaveClaim(Claimant claimant, EligibilityAndEntitlement eligibility) {
        Claim claim = buildClaim(claimant, eligibility);
        claimRepository.save(claim);
        log.info("Saved new claimant: {} with status {}", claim.getId(), claim.getEligibilityStatus());
        eventAuditor.auditNewClaim(claim);
        return claim;
    }

    private void sendNewCardMessage(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement) {
        NewCardRequestMessagePayload payload = buildNewCardMessagePayload(claim, voucherEntitlement);
        messageQueueDAO.sendMessage(payload, CREATE_NEW_CARD);
    }

    private Claim buildClaim(Claimant claimant, EligibilityAndEntitlement eligibility) {
        ClaimStatus claimStatus = STATUS_MAP.get(eligibility.getEligibilityStatus());
        LocalDateTime currentDateTime = LocalDateTime.now();
        return Claim.builder()
                .dwpHouseholdIdentifier(eligibility.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(eligibility.getHmrcHouseholdIdentifier())
                .eligibilityStatus(eligibility.getEligibilityStatus())
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
        VoucherEntitlement firstVoucherEntitlement
                = min(voucherEntitlement.getVoucherEntitlements(), Comparator.comparing(VoucherEntitlement::getEntitlementDate));

        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(firstVoucherEntitlement))
                .build();
    }

}
