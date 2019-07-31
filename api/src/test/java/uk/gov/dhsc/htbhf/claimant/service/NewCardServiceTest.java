package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.factory.CardRequestFactory;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.NEW;
import static uk.gov.dhsc.htbhf.claimant.service.audit.FailedEventTestUtils.verifyCardCreationEventFailExceptionAndEventAreCorrectWithCardId;
import static uk.gov.dhsc.htbhf.claimant.service.audit.FailedEventTestUtils.verifyCardCreationEventFailExceptionAndEventAreCorrectWithoutCardId;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aValidCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION;

@ExtendWith(MockitoExtension.class)
class NewCardServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @Mock
    private CardClient cardClient;

    @Mock
    private CardRequestFactory cardRequestFactory;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private EventAuditor eventAuditor;

    @Mock
    private Clock clock;

    @InjectMocks
    private NewCardService newCardService;

    @Test
    void shouldCallCardClientAndUpdateClaim() {
        CardRequest cardRequest = aValidCardRequest();
        CardResponse cardResponse = aCardResponse();
        given(cardRequestFactory.createCardRequest(any())).willReturn(cardRequest);
        given(cardClient.requestNewCard(any())).willReturn(cardResponse);
        given(clock.instant()).willReturn(fixedClock.instant());
        given(clock.getZone()).willReturn(fixedClock.getZone());
        Claim claim = aClaimWithClaimStatus(NEW);

        newCardService.createNewCard(claim);

        verify(cardRequestFactory).createCardRequest(claim);
        verify(cardClient).requestNewCard(cardRequest);
        verify(eventAuditor).auditNewCard(claim.getId(), cardResponse);
        ArgumentCaptor<Claim> argumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(argumentCaptor.capture());
        assertThat(claim.getCardAccountId()).isEqualTo(cardResponse.getCardAccountId());
        assertThat(claim.getClaimStatus()).isEqualTo(ACTIVE);
        assertThat(claim.getClaimStatusTimestamp()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void shouldThrowEventFailedExceptionWhenFailingToCreateANewCard() {
        Claim claim = aValidClaimBuilder()
                .claimStatus(NEW)
                .cardAccountId(null)
                .build();
        CardRequest cardRequest = aValidCardRequest();
        given(cardRequestFactory.createCardRequest(any())).willReturn(cardRequest);
        given(cardClient.requestNewCard(any())).willThrow(TEST_EXCEPTION);

        EventFailedException exception = catchThrowableOfType(() -> newCardService.createNewCard(claim), EventFailedException.class);

        assertThat(claim.getClaimStatus()).isEqualTo(NEW);
        assertThat(claim.getCardAccountId()).isNull();

        verifyCardCreationEventFailExceptionAndEventAreCorrectWithoutCardId(claim, TEST_EXCEPTION, exception);
        verify(cardRequestFactory).createCardRequest(claim);
    }

    @Test
    void shouldThrowEventFailedExceptionWhenFailingToSaveUpdatedClaimAfterCardCreation() {
        Claim claim = aValidClaimBuilder()
                .claimStatus(NEW)
                .cardAccountId(null)
                .build();
        CardRequest cardRequest = aValidCardRequest();
        given(cardRequestFactory.createCardRequest(any())).willReturn(cardRequest);
        CardResponse cardResponse = aCardResponse();
        given(cardClient.requestNewCard(any())).willReturn(cardResponse);
        doThrow(TEST_EXCEPTION).when(claimRepository).save(any());
        given(clock.instant()).willReturn(fixedClock.instant());
        given(clock.getZone()).willReturn(fixedClock.getZone());

        EventFailedException exception = catchThrowableOfType(() -> newCardService.createNewCard(claim), EventFailedException.class);

        //simulating save failed so Claim will be modified but not saved as save() threw an Exception
        assertThat(claim.getClaimStatus()).isEqualTo(ACTIVE);
        assertThat(claim.getCardAccountId()).isEqualTo(cardResponse.getCardAccountId());

        verifyCardCreationEventFailExceptionAndEventAreCorrectWithCardId(claim, TEST_EXCEPTION, exception, cardResponse.getCardAccountId());
        verify(cardRequestFactory).createCardRequest(claim);
        verify(cardClient).requestNewCard(cardRequest);
    }

}
