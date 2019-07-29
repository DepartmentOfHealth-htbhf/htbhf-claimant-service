package uk.gov.dhsc.htbhf.claimant.exception;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewCardEvent;

import static uk.gov.dhsc.htbhf.claimant.service.audit.FailedEventTestUtils.verifyCardCreationEventFailExceptionAndEventAreCorrect;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION;

class EventFailedExceptionTest {

    @Test
    void shouldCreateFailureEventOnConstruction() {
        //Given
        Claim claim = aValidClaim();
        NewCardEvent event = NewCardEvent.builder().claimId(claim.getId()).cardAccountId(CARD_ACCOUNT_ID).build();
        String failureMessage = String.format("Card creation failed for claim %s, exception is: test exception", claim.getId());
        RuntimeException testException = new RuntimeException("test exception");

        //When
        EventFailedException exception = new EventFailedException(event, testException, failureMessage);

        //Then
        verifyCardCreationEventFailExceptionAndEventAreCorrect(claim, TEST_EXCEPTION, exception,
                "EventFailedExceptionTest.shouldCreateFailureEventOnConstruction", CARD_ACCOUNT_ID);
    }
}
