package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.factory.CardRequestFactory;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aValidCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

@ExtendWith(MockitoExtension.class)
class NewCardServiceTest {

    @Mock
    private CardClient cardClient;

    @Mock
    private CardRequestFactory cardRequestFactory;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimAuditor claimAuditor;

    @Mock
    private PaymentCycleService paymentCycleService;

    @InjectMocks
    private NewCardService newCardService;

    @Test
    void shouldCallCardClientAndSetCardIdForNewClaim() {
        Claim claim = aValidClaim();
        CardRequest cardRequest = aValidCardRequest();
        CardResponse cardResponse = aCardResponse();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(cardRequestFactory.createCardRequest(any())).willReturn(cardRequest);
        given(cardClient.requestNewCard(any())).willReturn(cardResponse);

        newCardService.createNewCard(claim.getId());

        verify(cardRequestFactory).createCardRequest(claim);
        verify(cardClient).requestNewCard(cardRequest);
        verify(claimAuditor).auditNewCard(claim.getId(), cardResponse);
        ArgumentCaptor<Claim> argumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(argumentCaptor.capture());
        assertThat(claim.getCardAccountId()).isEqualTo(cardResponse.getCardAccountId());
        verify(paymentCycleService).createNewPaymentCycle(claim, claim.getClaimStatusTimestamp().toLocalDate());
    }

    @Test
    void shouldThrowExceptionWhenClaimIsNotFound() {
        UUID claimId = UUID.randomUUID();
        given(claimRepository.findById(any())).willReturn(Optional.empty());

        EntityNotFoundException exception = catchThrowableOfType(() -> newCardService.createNewCard(claimId),
                EntityNotFoundException.class);

        assertThat(exception.getMessage()).isEqualTo("Unable to find claim with id " + claimId);
        verify(claimRepository).findById(claimId);
        verifyNoMoreInteractions(claimRepository);
    }
}
