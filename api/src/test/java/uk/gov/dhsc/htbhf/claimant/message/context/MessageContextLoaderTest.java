package uk.gov.dhsc.htbhf.claimant.message.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessingException;
import uk.gov.dhsc.htbhf.claimant.message.payload.DetermineEntitlementMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class MessageContextLoaderTest {

    public static final String CARD_ACCOUNT_ID = "myCardAccountId";
    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PaymentCycleRepository paymentCycleRepository;

    @InjectMocks
    private MessageContextLoader loader;

    //These calls need to be lenient as Mockito sees mocking the same method call multiple times as a sign of an error
    //and will fail the test without setting the mode to lenient.
    @Test
    void shouldSuccessfullyLoadPaymentContext() {
        //Given
        Claim claim = ClaimTestDataFactory.aValidClaim();
        UUID claimId = claim.getId();
        PaymentCycle paymentCycle = aValidPaymentCycle();
        UUID paymentCycleId = paymentCycle.getId();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        when(paymentCycleRepository.findById(paymentCycleId)).thenReturn(Optional.of(paymentCycle));
        MakePaymentMessagePayload payload = buildPaymentPayload(claimId, paymentCycleId, CARD_ACCOUNT_ID);

        //When
        MakePaymentMessageContext context = loader.loadContext(payload);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getPaymentCycle()).isEqualTo(paymentCycle);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(paymentCycleId);
        verifyNoMoreInteractions(paymentCycleRepository, claimRepository);
    }

    @Test
    void shouldFailToLoadPaymentContextIfClaimNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        PaymentCycle paymentCycle = aValidPaymentCycle();
        UUID paymentCycleId = paymentCycle.getId();
        when(paymentCycleRepository.findById(paymentCycleId)).thenReturn(Optional.of(paymentCycle));
        given(claimRepository.findById(any())).willReturn(Optional.empty());
        when(paymentCycleRepository.findById(paymentCycleId)).thenReturn(Optional.of(paymentCycle));
        MakePaymentMessagePayload payload = buildPaymentPayload(claimId, paymentCycleId, CARD_ACCOUNT_ID);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadContext(payload), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load claim using id: " + claimId);
        verify(claimRepository).findById(claimId);
    }

    @Test
    void shouldFailToLoadPaymentContextIfPaymentCycleNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        UUID paymentCycleId = UUID.randomUUID();
        when(paymentCycleRepository.findById(paymentCycleId)).thenReturn(Optional.empty());
        MakePaymentMessagePayload payload = buildPaymentPayload(claimId, paymentCycleId, CARD_ACCOUNT_ID);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadContext(payload), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load payment cycle using id: "
                + paymentCycleId);
        verify(paymentCycleRepository).findById(paymentCycleId);
    }


    //These calls need to be lenient as Mockito sees mocking the same method call multiple times as a sign of an error
    //and will fail the test without setting the mode to lenient.
    @Test
    void shouldSuccessfullyLoadEntitlementContext() {
        //Given
        Claim claim = ClaimTestDataFactory.aValidClaim();
        UUID claimId = claim.getId();
        PaymentCycle previousPaymentCycle = aValidPaymentCycle();
        UUID previousPaymentCycleId = previousPaymentCycle.getId();
        PaymentCycle currentPaymentCycle = aValidPaymentCycle();
        UUID currentPaymentCycleId = currentPaymentCycle.getId();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        lenient().when(paymentCycleRepository.findById(currentPaymentCycleId)).thenReturn(Optional.of(currentPaymentCycle));
        lenient().when(paymentCycleRepository.findById(previousPaymentCycleId)).thenReturn(Optional.of(previousPaymentCycle));
        DetermineEntitlementMessagePayload payload = buildEntitlementPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);

        //When
        DetermineEntitlementMessageContext context = loader.loadContext(payload);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getCurrentPaymentCycle()).isEqualTo(currentPaymentCycle);
        assertThat(context.getPreviousPaymentCycle()).isEqualTo(previousPaymentCycle);
        assertThat(previousPaymentCycleId).isNotEqualTo(currentPaymentCycleId);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(previousPaymentCycleId);
        verifyNoMoreInteractions(paymentCycleRepository, claimRepository);
    }

    @Test
    void shouldFailToLoadEntitlementContextIfClaimNotFound() {
        //Given
        Claim claim = ClaimTestDataFactory.aValidClaim();
        UUID claimId = claim.getId();
        PaymentCycle previousPaymentCycle = aValidPaymentCycle();
        UUID previousPaymentCycleId = previousPaymentCycle.getId();
        PaymentCycle currentPaymentCycle = aValidPaymentCycle();
        UUID currentPaymentCycleId = currentPaymentCycle.getId();
        given(claimRepository.findById(any())).willReturn(Optional.empty());
        lenient().when(paymentCycleRepository.findById(currentPaymentCycleId)).thenReturn(Optional.of(currentPaymentCycle));
        lenient().when(paymentCycleRepository.findById(previousPaymentCycleId)).thenReturn(Optional.of(previousPaymentCycle));
        DetermineEntitlementMessagePayload payload = buildEntitlementPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadContext(payload), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load claim using id: " + claimId);
        assertThat(previousPaymentCycleId).isNotEqualTo(currentPaymentCycleId);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(previousPaymentCycleId);
    }

    @Test
    void shouldFailToLoadEntitlementContextIfCurrentPaymentCycleNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        UUID previousPaymentCycleId = UUID.randomUUID();
        UUID currentPaymentCycleId = UUID.randomUUID();
        lenient().when(paymentCycleRepository.findById(currentPaymentCycleId)).thenReturn(Optional.empty());
        DetermineEntitlementMessagePayload payload = buildEntitlementPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadContext(payload), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load current payment cycle using id: "
                + currentPaymentCycleId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
    }

    @Test
    void shouldFailToLoadEntitlementContextIfPreviousPaymentCycleNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        UUID previousPaymentCycleId = UUID.randomUUID();
        PaymentCycle currentPaymentCycle = aValidPaymentCycle();
        UUID currentPaymentCycleId = currentPaymentCycle.getId();
        lenient().when(paymentCycleRepository.findById(currentPaymentCycleId)).thenReturn(Optional.of(currentPaymentCycle));
        lenient().when(paymentCycleRepository.findById(previousPaymentCycleId)).thenReturn(Optional.empty());
        DetermineEntitlementMessagePayload payload = buildEntitlementPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadContext(payload), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load previous payment cycle using id: "
                + previousPaymentCycleId);
        assertThat(previousPaymentCycleId).isNotEqualTo(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(previousPaymentCycleId);
    }

    private DetermineEntitlementMessagePayload buildEntitlementPayload(UUID claimId, UUID previousPaymentCycleId, UUID currentPaymentCycleId) {
        return DetermineEntitlementMessagePayload.builder()
                .claimId(claimId)
                .currentPaymentCycleId(currentPaymentCycleId)
                .previousPaymentCycleId(previousPaymentCycleId)
                .build();
    }

    private MakePaymentMessagePayload buildPaymentPayload(UUID claimId, UUID paymentCycleId, String cardAccountId) {
        return MakePaymentMessagePayload.builder()
                .claimId(claimId)
                .paymentCycleId(paymentCycleId)
                .cardAccountId(cardAccountId)
                .build();
    }
}
