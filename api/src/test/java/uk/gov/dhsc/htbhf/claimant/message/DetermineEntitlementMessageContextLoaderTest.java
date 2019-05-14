package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.DetermineEntitlementMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class DetermineEntitlementMessageContextLoaderTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PaymentCycleRepository paymentCycleRepository;

    @InjectMocks
    private DetermineEntitlementMessageContextLoader loader;

    //These calls need to be lenient as Mockito sees mocking the same method call multiple times as a sign of an error
    //and will fail the test without setting the mode to lenient.
    @Test
    void shouldSuccessfullyLoadContext() {
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
        DetermineEntitlementMessagePayload payload = buildPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);

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
    void shouldFailToLoadContextIfClaimNotFound() {
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
        DetermineEntitlementMessagePayload payload = buildPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadContext(payload), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process DETERMINE_ENTITLEMENT message, unable to load claim using id: " + claimId);
        assertThat(previousPaymentCycleId).isNotEqualTo(currentPaymentCycleId);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(previousPaymentCycleId);
    }

    @Test
    void shouldFailToLoadContextIfCurrentPaymentCycleNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        UUID previousPaymentCycleId = UUID.randomUUID();
        UUID currentPaymentCycleId = UUID.randomUUID();
        lenient().when(paymentCycleRepository.findById(currentPaymentCycleId)).thenReturn(Optional.empty());
        DetermineEntitlementMessagePayload payload = buildPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadContext(payload), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process DETERMINE_ENTITLEMENT message, unable to load current payment cycle using id: "
                + currentPaymentCycleId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
    }

    @Test
    void shouldFailToLoadContextIfPreviousPaymentCycleNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        UUID previousPaymentCycleId = UUID.randomUUID();
        PaymentCycle currentPaymentCycle = aValidPaymentCycle();
        UUID currentPaymentCycleId = currentPaymentCycle.getId();
        lenient().when(paymentCycleRepository.findById(currentPaymentCycleId)).thenReturn(Optional.of(currentPaymentCycle));
        lenient().when(paymentCycleRepository.findById(previousPaymentCycleId)).thenReturn(Optional.empty());
        DetermineEntitlementMessagePayload payload = buildPayload(claimId, previousPaymentCycleId, currentPaymentCycleId);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadContext(payload), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process DETERMINE_ENTITLEMENT message, unable to load previous payment cycle using id: "
                + previousPaymentCycleId);
        assertThat(previousPaymentCycleId).isNotEqualTo(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(currentPaymentCycleId);
        verify(paymentCycleRepository).findById(previousPaymentCycleId);
    }

    private DetermineEntitlementMessagePayload buildPayload(UUID claimId, UUID previousPaymentCycleId, UUID currentPaymentCycleId) {
        return DetermineEntitlementMessagePayload.builder()
                .claimId(claimId)
                .currentPaymentCycleId(currentPaymentCycleId)
                .previousPaymentCycleId(previousPaymentCycleId)
                .build();
    }
}
