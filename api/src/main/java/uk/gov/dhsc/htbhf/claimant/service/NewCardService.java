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
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

@Service
@AllArgsConstructor
@Slf4j
public class NewCardService {

    private CardClient cardClient;
    private CardRequestFactory cardRequestFactory;
    private ClaimRepository claimRepository;
    private ClaimAuditor claimAuditor;
    private PaymentCycleService paymentCycleService;

    @Transactional
    public void createNewCard(Claim claim) {
        CardRequest cardRequest = cardRequestFactory.createCardRequest(claim);
        CardResponse cardResponse = cardClient.requestNewCard(cardRequest);
        saveClaimWithCardId(claim, cardResponse);
        claimAuditor.auditNewCard(claim.getId(), cardResponse);
        paymentCycleService.createAndSavePaymentCycle(claim, claim.getClaimStatusTimestamp().toLocalDate());
    }

    private void saveClaimWithCardId(Claim claim, CardResponse cardResponse) {
        claim.setCardAccountId(cardResponse.getCardAccountId());
        claimRepository.save(claim);
    }
}
