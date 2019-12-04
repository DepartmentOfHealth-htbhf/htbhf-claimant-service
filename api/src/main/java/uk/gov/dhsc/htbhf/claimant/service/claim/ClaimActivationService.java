package uk.gov.dhsc.htbhf.claimant.service.claim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;

import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_NEW_TO_ACTIVE;

@Service
@AllArgsConstructor
@Slf4j
public class ClaimActivationService {

    private ClaimRepository claimRepository;
    private PaymentCycleService paymentCycleService;
    private EventAuditor eventAuditor;
    private ClaimMessageSender claimMessageSender;

    /**
     * Saves the new cardAccountId on the claim, sets the status of the claim to active, and creates a new {@link PaymentCycle}.
     * Also records an audit event for the new card, and sends a message to report the change in claim status from new to active.
     * @param claim the claim to update
     * @param cardAccountId the account id
     * @param decision used to create the new {@link PaymentCycle}.
     * @return the first payment cycle for the claim.
     */
    public PaymentCycle updateClaimAndCreatePaymentCycle(Claim claim,
                                                         String cardAccountId,
                                                         EligibilityAndEntitlementDecision decision) {
        LocalDate firstCycleStartDate = claim.getClaimStatusTimestamp().toLocalDate();
        updateClaim(claim, cardAccountId);
        reportUpdatedClaim(claim, cardAccountId);
        return paymentCycleService.createAndSavePaymentCycleForEligibleClaim(claim, firstCycleStartDate, decision);
    }

    private void updateClaim(Claim claim, String cardAccountId) {
        claim.setCardAccountId(cardAccountId);
        claim.updateClaimStatus(ACTIVE);
        claimRepository.save(claim);
    }

    private void reportUpdatedClaim(Claim claim, String cardAccountId) {
        eventAuditor.auditNewCard(claim.getId(), cardAccountId);
        claimMessageSender.sendReportClaimMessage(claim, UPDATED_FROM_NEW_TO_ACTIVE);
    }

}
