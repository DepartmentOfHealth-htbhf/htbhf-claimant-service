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

import java.time.Clock;
import java.time.LocalDateTime;

import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;

@Service
@AllArgsConstructor
@Slf4j
public class NewCardService {

    private CardClient cardClient;
    private CardRequestFactory cardRequestFactory;
    private ClaimRepository claimRepository;
    private EventAuditor eventAuditor;
    private Clock clock;

    /**
     * Creates a new card for the given claim. If successful, the card account id is saved to the claim and the claim status is set to ACTIVE.
     * <p>
     * Note that the PMD warning is suppressed so that we can use the value of CardResponse in the construction
     * of the failed new card event if it has been set.
     * </p>
     *
     * @param claim the claim to create a new card for.
     */
    @SuppressWarnings("PMD.NullAssignment")
    @Transactional
    public void createNewCard(Claim claim) {
        CardResponse cardResponse = null;
        try {
            CardRequest cardRequest = cardRequestFactory.createCardRequest(claim);
            cardResponse = cardClient.requestNewCard(cardRequest);
            updateAndSaveClaim(claim, cardResponse);
            eventAuditor.auditNewCard(claim.getId(), cardResponse);
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
        claim.setClaimStatus(ACTIVE);
        claim.setClaimStatusTimestamp(LocalDateTime.now(clock));
        claimRepository.save(claim);
    }
}
