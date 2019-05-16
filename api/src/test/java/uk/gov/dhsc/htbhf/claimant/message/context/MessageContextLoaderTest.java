package uk.gov.dhsc.htbhf.claimant.message.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessingException;
import uk.gov.dhsc.htbhf.claimant.message.PayloadMapper;
import uk.gov.dhsc.htbhf.claimant.message.payload.DetermineEntitlementMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class MessageContextLoaderTest {

    public static final String CARD_ACCOUNT_ID = "myCardAccountId";
    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PaymentCycleRepository paymentCycleRepository;

    @Mock
    private PayloadMapper payloadMapper;

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
        given(paymentCycleRepository.findById(any())).willReturn(Optional.of(paymentCycle));
        MakePaymentMessagePayload payload = buildPaymentPayload(claimId, paymentCycleId);
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MakePaymentMessageContext context = loader.loadMakePaymentContext(message);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getPaymentCycle()).isEqualTo(paymentCycle);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(paymentCycleId);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
        verifyNoMoreInteractions(paymentCycleRepository, claimRepository, payloadMapper);
    }

    @Test
    void shouldFailToLoadPaymentContextIfClaimNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        PaymentCycle paymentCycle = aValidPaymentCycle();
        UUID paymentCycleId = paymentCycle.getId();
        given(paymentCycleRepository.findById(any())).willReturn(Optional.of(paymentCycle));
        given(claimRepository.findById(any())).willReturn(Optional.empty());
        MakePaymentMessagePayload payload = buildPaymentPayload(claimId, paymentCycleId);
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadMakePaymentContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load claim using id: " + claimId);
        verify(claimRepository).findById(claimId);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
    }

    @Test
    void shouldFailToLoadPaymentContextIfPaymentCycleNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        UUID paymentCycleId = UUID.randomUUID();
        given(paymentCycleRepository.findById(any())).willReturn(Optional.empty());
        MakePaymentMessagePayload payload = buildPaymentPayload(claimId, paymentCycleId);
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadMakePaymentContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load payment cycle using id: " + paymentCycleId);
        verify(paymentCycleRepository).findById(paymentCycleId);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
    }

    @Test
    void shouldFailToLoadPaymentContextIfUnableToMapPayload() {
        //Given
        MessageProcessingException testException = new MessageProcessingException("Unable to load payload");
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willThrow(testException);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadMakePaymentContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).isEqualTo(testException);
        verifyZeroInteractions(paymentCycleRepository, claimRepository);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
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
        given(payloadMapper.getPayload(any(), eq(DetermineEntitlementMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        DetermineEntitlementMessageContext context = loader.loadDetermineEntitlementContext(message);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getCurrentPaymentCycle()).isEqualTo(currentPaymentCycle);
        assertThat(context.getPreviousPaymentCycle()).isEqualTo(previousPaymentCycle);
        assertThat(previousPaymentCycleId).isNotEqualTo(currentPaymentCycleId);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(previousPaymentCycleId);
        verify(payloadMapper).getPayload(message, DetermineEntitlementMessagePayload.class);
        verifyNoMoreInteractions(paymentCycleRepository, claimRepository, payloadMapper);
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
        given(payloadMapper.getPayload(any(), eq(DetermineEntitlementMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadDetermineEntitlementContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load claim using id: " + claimId);
        assertThat(previousPaymentCycleId).isNotEqualTo(currentPaymentCycleId);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(previousPaymentCycleId);
        verify(payloadMapper).getPayload(message, DetermineEntitlementMessagePayload.class);
    }

    @Test
    void shouldFailToLoadEntitlementContextIfCurrentPaymentCycleNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        UUID previousPaymentCycleId = UUID.randomUUID();
        UUID currentPaymentCycleId = UUID.randomUUID();
        lenient().when(paymentCycleRepository.findById(currentPaymentCycleId)).thenReturn(Optional.empty());
        DetermineEntitlementMessagePayload payload = buildEntitlementPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);
        given(payloadMapper.getPayload(any(), eq(DetermineEntitlementMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadDetermineEntitlementContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load current payment cycle using id: "
                + currentPaymentCycleId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(payloadMapper).getPayload(message, DetermineEntitlementMessagePayload.class);
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
        given(payloadMapper.getPayload(any(), eq(DetermineEntitlementMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadDetermineEntitlementContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load previous payment cycle using id: "
                + previousPaymentCycleId);
        assertThat(previousPaymentCycleId).isNotEqualTo(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(previousPaymentCycleId);
        verify(payloadMapper).getPayload(message, DetermineEntitlementMessagePayload.class);
    }

    @Test
    void shouldSuccessfullyLoadMakePaymentMessagePayload() {
        //Given
        Claim claim = ClaimTestDataFactory.aValidClaim();
        UUID claimId = claim.getId();
        PaymentCycle paymentCycle = aValidPaymentCycle();
        UUID paymentCycleId = paymentCycle.getId();
        String cardAccountId = "cardId";
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(paymentCycleRepository.findById(paymentCycleId)).willReturn(Optional.of(paymentCycle));
        MakePaymentMessagePayload payload = buildMakePaymentPayload(claimId, paymentCycleId, cardAccountId);
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MakePaymentMessageContext context = loader.loadMakePaymentContext(message);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getCardAccountId()).isEqualTo(cardAccountId);
        assertThat(context.getPaymentCycle()).isEqualTo(paymentCycle);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(paymentCycleId);
    }

    @Test
    void shouldFailToMakePaymentContextIfClaimNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        PaymentCycle paymentCycle = aValidPaymentCycle();
        UUID paymentCycleId = paymentCycle.getId();
        String cardAccountId = "cardId";
        given(claimRepository.findById(any())).willReturn(Optional.empty());
        given(paymentCycleRepository.findById(paymentCycleId)).willReturn(Optional.of(paymentCycle));
        MakePaymentMessagePayload payload = buildMakePaymentPayload(claimId, paymentCycleId, cardAccountId);
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadMakePaymentContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load claim using id: " + claimId);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(paymentCycleId);
    }

    @Test
    void shouldFailToMakePaymentContextIfPaymentCycleNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        UUID paymentCycleId = UUID.randomUUID();
        String cardAccountId = "cardId";
        given(paymentCycleRepository.findById(paymentCycleId)).willReturn(Optional.empty());
        MakePaymentMessagePayload payload = buildMakePaymentPayload(claimId, paymentCycleId, cardAccountId);
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadMakePaymentContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load payment cycle using id: " + paymentCycleId);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
        verify(paymentCycleRepository).findById(paymentCycleId);
    }

    private DetermineEntitlementMessagePayload buildEntitlementPayload(UUID claimId, UUID previousPaymentCycleId, UUID currentPaymentCycleId) {
        return DetermineEntitlementMessagePayload.builder()
                .claimId(claimId)
                .currentPaymentCycleId(currentPaymentCycleId)
                .previousPaymentCycleId(previousPaymentCycleId)
                .build();
    }

    private MakePaymentMessagePayload buildMakePaymentPayload(UUID claimId, UUID paymentCycleId, String cardAccountId) {
        return MakePaymentMessagePayload.builder()
                .claimId(claimId)
                .cardAccountId(cardAccountId)
                .paymentCycleId(paymentCycleId)
                .build();
    }

    private MakePaymentMessagePayload buildPaymentPayload(UUID claimId, UUID paymentCycleId) {
        return MakePaymentMessagePayload.builder()
                .claimId(claimId)
                .paymentCycleId(paymentCycleId)
                .cardAccountId(CARD_ACCOUNT_ID)
                .build();
    }
}
