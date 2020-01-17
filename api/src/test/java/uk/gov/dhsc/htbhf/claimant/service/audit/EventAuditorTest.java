package uk.gov.dhsc.htbhf.claimant.service.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.EventLogger;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.ADDRESS;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.EXPECTED_DELIVERY_DATE;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.LAST_NAME;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

@ExtendWith(MockitoExtension.class)
class EventAuditorTest {

    @Mock
    private EventLogger eventLogger;

    @InjectMocks
    private EventAuditor eventAuditor;

    private LocalDateTime testStart = LocalDateTime.now();

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
        assertThat(actualEvent.getTimestamp()).isAfter(testStart);
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
        verifyNoMoreInteractions(eventLogger);
    }

    @Test
    void shouldLogUpdatedClaimEvent() {
        //Given
        Claim claim = aValidClaim();
        List<UpdatableClaimantField> updatedFields = List.of(EXPECTED_DELIVERY_DATE, LAST_NAME, ADDRESS);
        //When
        eventAuditor.auditUpdatedClaim(claim, updatedFields);
        //Then
        UUID claimId = claim.getId();
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(UPDATED_CLAIM);
        assertThat(actualEvent.getTimestamp()).isAfter(testStart);
        List<String> updatedFieldsAsStrings = List.of("expectedDeliveryDate", "lastName", "address");
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(2)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), claimId),
                        entry(ClaimEventMetadataKey.UPDATED_FIELDS.getKey(), updatedFieldsAsStrings));
    }

    @Test
    void shouldLogEventForNewCard() {
        //Given
        UUID claimId = UUID.randomUUID();

        //When
        eventAuditor.auditNewCard(claimId, CARD_ACCOUNT_ID);

        //Then
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(NEW_CARD);
        assertThat(actualEvent.getTimestamp()).isAfter(testStart);
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(2)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CARD_ACCOUNT_ID.getKey(), CARD_ACCOUNT_ID),
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), claimId));

    }

    @Test
    void shouldNotLogEventForNullClaimId() {
        //When
        eventAuditor.auditNewCard(null, CARD_ACCOUNT_ID);

        //Then
        verifyNoMoreInteractions(eventLogger);
    }

    @Test
    void shouldNotLogEventForNullCardResponse() {
        //When
        eventAuditor.auditNewCard(UUID.randomUUID(), null);

        //Then
        verifyNoMoreInteractions(eventLogger);
    }

    @Test
    void shouldLogEventForMakePayment() {
        //Given
        PaymentCycle paymentCycle = aValidPaymentCycle();
        int paymentAmount = 100;
        String requestReference = UUID.randomUUID().toString();
        String responseReference = UUID.randomUUID().toString();

        //When
        eventAuditor.auditMakePayment(paymentCycle, paymentAmount, requestReference, responseReference);

        //Then
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(MAKE_PAYMENT);
        assertThat(actualEvent.getTimestamp()).isAfter(testStart);
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(5)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), paymentCycle.getClaim().getId()),
                        entry(ClaimEventMetadataKey.ENTITLEMENT_AMOUNT_IN_PENCE.getKey(), paymentCycle.getTotalEntitlementAmountInPence()),
                        entry(ClaimEventMetadataKey.PAYMENT_AMOUNT.getKey(), paymentAmount),
                        entry(ClaimEventMetadataKey.PAYMENT_REQUEST_REFERENCE.getKey(), requestReference),
                        entry(ClaimEventMetadataKey.PAYMENT_RESPONSE_REFERENCE.getKey(), responseReference));
    }

    @Test
    void shouldLogEventForBalanceTooHighForPayment() {
        //Given
        int balanceOnCard = 9550;
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder().cardBalanceInPence(balanceOnCard).build();

        //When
        eventAuditor.auditBalanceTooHighForPayment(paymentCycle);

        //Then
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(BALANCE_TOO_HIGH_FOR_PAYMENT);
        assertThat(actualEvent.getTimestamp()).isAfter(testStart);
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(4)
                .containsExactly(
                        entry(ClaimEventMetadataKey.BALANCE_ON_CARD.getKey(), balanceOnCard),
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), paymentCycle.getClaim().getId()),
                        entry(ClaimEventMetadataKey.ENTITLEMENT_AMOUNT_IN_PENCE.getKey(), TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE),
                        entry(ClaimEventMetadataKey.PAYMENT_AMOUNT.getKey(), 0));

    }

    @Test
    void shouldLogClaimExpiredEvent() {
        //Given
        Claim claim = aValidClaim();

        //When
        eventAuditor.auditExpiredClaim(claim);

        //Then
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(EXPIRED_CLAIM);
        assertThat(actualEvent.getTimestamp()).isAfter(testStart);
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(1)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), claim.getId()));

    }

    @Test
    void shouldNotLogClaimExpiredWithNullClaim() {
        //Given
        Claim claim = null;
        //When
        eventAuditor.auditExpiredClaim(claim);
        //Then
        verifyNoMoreInteractions(eventLogger);

    }
}
