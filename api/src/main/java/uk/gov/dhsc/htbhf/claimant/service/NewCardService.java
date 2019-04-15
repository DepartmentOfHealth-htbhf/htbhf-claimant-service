package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.factory.CardRequestFactory;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class NewCardService {

    private CardClient cardClient;
    private CardRequestFactory cardRequestFactory;
    private ClaimRepository claimRepository;

    public void createNewCards(List<UUID> claimIds) {
        // TODO handle claims not found better
        claimIds.stream()
                .map(claimId -> claimRepository.findById(claimId).orElseThrow(RuntimeException::new))
                .map(claim -> cardRequestFactory.createCardRequest(claim))
                .forEach(cardRequest -> cardClient.createNewCardRequest(cardRequest));

    }
}
