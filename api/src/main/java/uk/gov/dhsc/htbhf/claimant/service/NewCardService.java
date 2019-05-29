package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.factory.CardRequestFactory;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;

import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;

@Service
@AllArgsConstructor
@Slf4j
public class NewCardService {

    private CardClient cardClient;
    private CardRequestFactory cardRequestFactory;
    private ClaimRepository claimRepository;
    private EventAuditor eventAuditor;

    /**
     * Creates a new card for the given claim. If successful, the card account id is saved to the claim and the claim status is set to ACTIVE.
     * @param claim the claim to create a new card for.
     */
    @Transactional
    public void createNewCard(Claim claim) {
        CardRequest cardRequest = cardRequestFactory.createCardRequest(claim);
        CardResponse cardResponse = cardClient.requestNewCard(cardRequest);
        updateAndSaveClaim(claim, cardResponse);
        eventAuditor.auditNewCard(claim.getId(), cardResponse);
    }

    private void updateAndSaveClaim(Claim claim, CardResponse cardResponse) {
        claim.setCardAccountId(cardResponse.getCardAccountId());
        claim.setClaimStatus(ACTIVE);
        claimRepository.save(claim);
    }
}
