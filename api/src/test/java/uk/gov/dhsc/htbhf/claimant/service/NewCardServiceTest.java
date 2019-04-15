package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.factory.CardRequestFactory;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aValidCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

@ExtendWith(MockitoExtension.class)
class NewCardServiceTest {

    @Mock
    private CardClient cardClient;

    @Mock
    private CardRequestFactory cardRequestFactory;

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private NewCardService newCardService;

    @Test
    void shouldCallCardClientForEachNewClaim() {
        List<UUID> claimIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        Claim claim = aValidClaim();
        CardRequest cardRequest = aValidCardRequest();
        given(cardRequestFactory.createCardRequest(any())).willReturn(cardRequest);
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));

        newCardService.createNewCards(claimIds);

        verify(cardRequestFactory, times(2)).createCardRequest(claim);
        verify(cardClient, times(2)).createNewCardRequest(cardRequest);
    }
}
