package uk.gov.dhsc.htbhf.claimant.service.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.EventLogger;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType.BALANCE_TOO_HIGH_FOR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType.NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType.NEW_CLAIM;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.DepositFundsTestDataFactory.aValidDepositFundsResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentTestDataFactory.aValidPayment;

@ExtendWith(MockitoExtension.class)
class EventAuditorTest {

    @Mock
    private EventLogger eventLogger;

    @InjectMocks
    private EventAuditor eventAuditor;

    @Test
    void shouldLogEventForValidClaimant() {
        //Given
        Claim claim = aValidClaim();
        //When
        eventAuditor.auditNewClaim(claim);
        //Then
        UUID claimId = claim.getId();
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(NEW_CLAIM);
        assertThat(actualEvent.getTimestamp()).isNotNull();
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(3)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), claimId),
                        entry(ClaimEventMetadataKey.CLAIM_STATUS.getKey(), ClaimStatus.ACTIVE),
                        entry(ClaimEventMetadataKey.ELIGIBILITY_STATUS.getKey(), EligibilityStatus.ELIGIBLE));
    }

    @Test
    void shouldNotLogEventForNullClaimant() {
        //When
        eventAuditor.auditNewClaim(null);
        //Then
        verifyZeroInteractions(eventLogger);
    }

    @Test
    void shouldLogEventForNewCard() {
        //Given
        UUID claimId = UUID.randomUUID();
        CardResponse cardResponse = aCardResponse();

        //When
        eventAuditor.auditNewCard(claimId, cardResponse);

        //Then
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(NEW_CARD);
        assertThat(actualEvent.getTimestamp()).isNotNull();
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(2)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CARD_ACCOUNT_ID.getKey(), cardResponse.getCardAccountId()),
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), claimId));

    }

    @Test
    void shouldNotLogEventForNullClaimId() {
        //When
        eventAuditor.auditNewCard(null, aCardResponse());

        //Then
        verifyZeroInteractions(eventLogger);
    }

    @Test
    void shouldNotLogEventForNullCardResponse() {
        //When
        eventAuditor.auditNewCard(UUID.randomUUID(), null);

        //Then
        verifyZeroInteractions(eventLogger);
    }

    @Test
    void shouldLogEventForMakePayment() {
        //Given
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Payment payment = aValidPayment();
        DepositFundsResponse depositFundsResponse = aValidDepositFundsResponse();

        //When
        eventAuditor.auditMakePayment(paymentCycle, payment, depositFundsResponse);

        //Then
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(MAKE_PAYMENT);
        assertThat(actualEvent.getTimestamp()).isNotNull();
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(5)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), paymentCycle.getClaim().getId()),
                        entry(ClaimEventMetadataKey.ENTITLEMENT_AMOUNT_IN_PENCE.getKey(), paymentCycle.getTotalEntitlementAmountInPence()),
                        entry(ClaimEventMetadataKey.PAYMENT_AMOUNT.getKey(), payment.getPaymentAmountInPence()),
                        entry(ClaimEventMetadataKey.PAYMENT_ID.getKey(), payment.getId()),
                        entry(ClaimEventMetadataKey.PAYMENT_REFERENCE.getKey(), depositFundsResponse.getReferenceId()));
    }

    @Test
    void shouldLogEventForBalanceTooHighForPayment() {
        //Given
        UUID claimId = UUID.randomUUID();
        int entitlementAmountInPence = 3450;
        int balanceOnCard = 9550;

        //When
        eventAuditor.auditBalanceTooHighForPayment(claimId, entitlementAmountInPence, balanceOnCard);

        //Then
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(BALANCE_TOO_HIGH_FOR_PAYMENT);
        assertThat(actualEvent.getTimestamp()).isNotNull();
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(3)
                .containsExactly(
                        entry(ClaimEventMetadataKey.BALANCE_ON_CARD.getKey(), balanceOnCard),
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), claimId),
                        entry(ClaimEventMetadataKey.ENTITLEMENT_AMOUNT_IN_PENCE.getKey(), entitlementAmountInPence));

    }
}
