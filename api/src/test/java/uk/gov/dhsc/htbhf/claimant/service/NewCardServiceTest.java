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
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.logging.event.CommonEventType;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.NEW;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory.aValidCardRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.EXCEPTION_DETAIL_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILED_EVENT_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILURE_DESCRIPTION_KEY;

@ExtendWith(MockitoExtension.class)
class NewCardServiceTest {

    @Mock
    private CardClient cardClient;

    @Mock
    private CardRequestFactory cardRequestFactory;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private EventAuditor eventAuditor;

    @InjectMocks
    private NewCardService newCardService;

    @Test
    void shouldCallCardClientAndUpdateClaim() {
        Claim claim = aClaimWithClaimStatus(NEW);
        CardRequest cardRequest = aValidCardRequest();
        CardResponse cardResponse = aCardResponse();
        given(cardRequestFactory.createCardRequest(any())).willReturn(cardRequest);
        given(cardClient.requestNewCard(any())).willReturn(cardResponse);

        newCardService.createNewCard(claim);

        verify(cardRequestFactory).createCardRequest(claim);
        verify(cardClient).requestNewCard(cardRequest);
        verify(eventAuditor).auditNewCard(claim.getId(), cardResponse);
        ArgumentCaptor<Claim> argumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(argumentCaptor.capture());
        assertThat(claim.getCardAccountId()).isEqualTo(cardResponse.getCardAccountId());
        assertThat(claim.getClaimStatus()).isEqualTo(ACTIVE);
    }

    @Test
    void shouldThrowEventFailedExceptionWhenFailingToCreateANewCard() {
        Claim claim = aValidClaimBuilder()
                .claimStatus(NEW)
                .cardAccountId(null)
                .build();
        CardRequest cardRequest = aValidCardRequest();
        given(cardRequestFactory.createCardRequest(any())).willReturn(cardRequest);
        RuntimeException testException = new RuntimeException("test exception");
        given(cardClient.requestNewCard(any())).willThrow(testException);

        EventFailedException exception = catchThrowableOfType(() -> newCardService.createNewCard(claim), EventFailedException.class);

        assertThat(claim.getClaimStatus()).isEqualTo(NEW);
        assertThat(claim.getCardAccountId()).isNull();

        verifyEventFailExceptionAndEventAreCorrect(claim, testException, exception, "NewCardService.createNewCard");
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
        RuntimeException testException = new RuntimeException("test exception");
        doThrow(testException).when(claimRepository).save(any());

        EventFailedException exception = catchThrowableOfType(() -> newCardService.createNewCard(claim), EventFailedException.class);

        //simulating save failed so Claim will be modified but not saved as save() threw an Exception
        assertThat(claim.getClaimStatus()).isEqualTo(ACTIVE);
        assertThat(claim.getCardAccountId()).isEqualTo(cardResponse.getCardAccountId());

        verifyEventFailExceptionAndEventAreCorrect(claim, testException, exception, "NewCardService.updateAndSaveClaim");
        verify(cardRequestFactory).createCardRequest(claim);
        verify(cardClient).requestNewCard(cardRequest);
    }

    private void verifyEventFailExceptionAndEventAreCorrect(Claim claim,
                                                            RuntimeException testException,
                                                            EventFailedException exception,
                                                            String stackLocation) {
        String expectedFailureMessage = String.format("Card creation failed for claim %s, exception is: test exception", claim.getId());
        assertThat(exception).hasMessage(expectedFailureMessage);
        assertThat(exception).hasCause(testException);
        FailureEvent failureEvent = exception.getFailureEvent();
        assertThat(failureEvent.getEventType()).isEqualTo(CommonEventType.FAILURE);
        assertThat(failureEvent.getTimestamp()).isNotNull();
        Map<String, Object> metadata = failureEvent.getEventMetadata();
        assertThat(metadata.get(CLAIM_ID.getKey())).isEqualTo(claim.getId());
        assertThat(metadata.get(FAILED_EVENT_KEY)).isEqualTo(ClaimEventType.NEW_CARD);
        assertThat(metadata.get(FAILURE_DESCRIPTION_KEY)).isEqualTo(expectedFailureMessage);
        String actualExceptionDetail = (String) metadata.get(EXCEPTION_DETAIL_KEY);
        assertThat(actualExceptionDetail).startsWith("test exception");
        assertThat(actualExceptionDetail).contains(stackLocation);
    }
}
