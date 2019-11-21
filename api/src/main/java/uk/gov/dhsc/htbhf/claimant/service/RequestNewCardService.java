package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.factory.CardRequestFactory;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewCardEvent;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_NEW_TO_ACTIVE;

@Service
@AllArgsConstructor
@Slf4j
public class RequestNewCardService {

    private CardClient cardClient;
    private CardRequestFactory cardRequestFactory;
    private ClaimRepository claimRepository;
    private EventAuditor eventAuditor;
    private ClaimMessageSender claimMessageSender;

    /**
     * Creates a new card for the given claim. If successful, the card account id is saved to the claim and the claim status is set to ACTIVE.
     * <p>
     * Note that the PMD warning is suppressed so that we can use the value of CardResponse in the construction
     * of the failed new card event if it has been set.
     * </p>
     *
     * @param claim the claim to create a new card for.
     * @param datesOfBirthOfChildren dates of birth of the claimant's children
     */
    @SuppressWarnings("PMD.NullAssignment")
    // TODO DW HTBHF-2457 Get the dates of birth of children from the claim once it's saved there.
    @Transactional
    public void createNewCard(Claim claim, List<LocalDate> datesOfBirthOfChildren) {
        CardResponse cardResponse = null;
        try {
            CardRequest cardRequest = cardRequestFactory.createCardRequest(claim);
            cardResponse = cardClient.requestNewCard(cardRequest);
            updateAndSaveClaim(claim, cardResponse);
            eventAuditor.auditNewCard(claim.getId(), cardResponse.getCardAccountId());
            claimMessageSender.sendReportClaimMessage(claim, datesOfBirthOfChildren, UPDATED_FROM_NEW_TO_ACTIVE);
        } catch (RuntimeException e) {
            String failureMessage = String.format("Card creation failed for claim %s, exception is: %s", claim.getId(), e.getMessage());
            throw new EventFailedException(buildFailedNewCardEvent(claim, cardResponse), e, failureMessage);
        }
    }

    @SuppressWarnings("PMD.NullAssignment")
    private NewCardEvent buildFailedNewCardEvent(Claim claim, CardResponse cardResponse) {
        return NewCardEvent.builder()
                .claimId(claim.getId())
                .cardAccountId((cardResponse == null) ? null : cardResponse.getCardAccountId())
                .build();
    }

    private void updateAndSaveClaim(Claim claim, CardResponse cardResponse) {
        claim.setCardAccountId(cardResponse.getCardAccountId());
        claim.updateClaimStatus(ACTIVE);
        claimRepository.save(claim);
    }
}
