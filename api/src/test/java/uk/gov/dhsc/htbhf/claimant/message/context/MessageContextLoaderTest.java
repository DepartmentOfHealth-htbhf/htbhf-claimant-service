package uk.gov.dhsc.htbhf.claimant.message.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessingException;
import uk.gov.dhsc.htbhf.claimant.message.PayloadMapper;
import uk.gov.dhsc.htbhf.claimant.message.payload.*;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EmailPersonalisationMapTestDataFactory.buildEmailPersonalisation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aMakePaymentPayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aNewCardRequestMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

@ExtendWith(MockitoExtension.class)
class MessageContextLoaderTest {

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
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        PaymentCycle paymentCycle = aValidPaymentCycle();
        UUID paymentCycleId = paymentCycle.getId();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(paymentCycleRepository.findById(any())).willReturn(Optional.of(paymentCycle));
        MakePaymentMessagePayload payload = aMakePaymentPayload(claimId, paymentCycleId);
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
        MakePaymentMessagePayload payload = aMakePaymentPayload(claimId, paymentCycleId);
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
        MakePaymentMessagePayload payload = aMakePaymentPayload(claimId, paymentCycleId);
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
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        PaymentCycle previousPaymentCycle = aValidPaymentCycle();
        UUID previousPaymentCycleId = previousPaymentCycle.getId();
        PaymentCycle currentPaymentCycle = aValidPaymentCycle();
        UUID currentPaymentCycleId = currentPaymentCycle.getId();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        lenient().when(paymentCycleRepository.findById(any()))
                .thenReturn(Optional.of(currentPaymentCycle))
                .thenReturn(Optional.of(previousPaymentCycle));
        DetermineEntitlementMessagePayload payload = DetermineEntitlementMessagePayload.builder()
                .claimId(claimId)
                .previousPaymentCycleId(previousPaymentCycleId)
                .currentPaymentCycleId(currentPaymentCycleId)
                .build();
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
        UUID claimId = UUID.randomUUID();
        PaymentCycle previousPaymentCycle = aValidPaymentCycle();
        UUID previousPaymentCycleId = previousPaymentCycle.getId();
        PaymentCycle currentPaymentCycle = aValidPaymentCycle();
        UUID currentPaymentCycleId = currentPaymentCycle.getId();
        given(claimRepository.findById(any())).willReturn(Optional.empty());
        lenient().when(paymentCycleRepository.findById(any()))
                .thenReturn(Optional.of(currentPaymentCycle))
                .thenReturn(Optional.of(previousPaymentCycle));
        DetermineEntitlementMessagePayload payload = DetermineEntitlementMessagePayload.builder()
                .claimId(claimId)
                .previousPaymentCycleId(previousPaymentCycleId)
                .currentPaymentCycleId(currentPaymentCycleId)
                .build();
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
        lenient().when(paymentCycleRepository.findById(any())).thenReturn(Optional.empty());
        DetermineEntitlementMessagePayload payload = DetermineEntitlementMessagePayload.builder()
                .claimId(claimId)
                .previousPaymentCycleId(previousPaymentCycleId)
                .currentPaymentCycleId(currentPaymentCycleId)
                .build();
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
        lenient().when(paymentCycleRepository.findById(any()))
                .thenReturn(Optional.of(currentPaymentCycle))
                .thenReturn(Optional.empty());
        DetermineEntitlementMessagePayload payload = DetermineEntitlementMessagePayload.builder()
                .claimId(claimId)
                .previousPaymentCycleId(previousPaymentCycleId)
                .currentPaymentCycleId(currentPaymentCycleId)
                .build();
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
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        PaymentCycle paymentCycle = aValidPaymentCycle();
        UUID paymentCycleId = paymentCycle.getId();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(paymentCycleRepository.findById(any())).willReturn(Optional.of(paymentCycle));
        MakePaymentMessagePayload payload = aMakePaymentPayload(claimId, paymentCycleId);
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MakePaymentMessageContext context = loader.loadMakePaymentContext(message);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getCardAccountId()).isEqualTo(CARD_ACCOUNT_ID);
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
        given(claimRepository.findById(any())).willReturn(Optional.empty());
        given(paymentCycleRepository.findById(any())).willReturn(Optional.of(paymentCycle));
        MakePaymentMessagePayload payload = aMakePaymentPayload(claimId, paymentCycleId);
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
        given(paymentCycleRepository.findById(any())).willReturn(Optional.empty());
        MakePaymentMessagePayload payload = aMakePaymentPayload(claimId, paymentCycleId);
        given(payloadMapper.getPayload(any(), eq(MakePaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadMakePaymentContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load payment cycle using id: " + paymentCycleId);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
        verify(paymentCycleRepository).findById(paymentCycleId);
    }

    @Test
    void shouldSuccessfullyLoadNewCardContext() {
        //Given
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        List<LocalDate> datesOfBirth = List.of(LocalDate.now().minusDays(1), LocalDate.now());
        NewCardRequestMessagePayload payload = aNewCardRequestMessagePayload(claimId, voucherEntitlement, datesOfBirth);
        given(payloadMapper.getPayload(any(), eq(NewCardRequestMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(CREATE_NEW_CARD);

        //When
        NewCardMessageContext context = loader.loadNewCardContext(message);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getPaymentCycleVoucherEntitlement()).isEqualTo(voucherEntitlement);
        assertThat(context.getDatesOfBirthOfChildren()).isEqualTo(datesOfBirth);
        verify(payloadMapper).getPayload(message, NewCardRequestMessagePayload.class);
        verify(claimRepository).findById(claimId);
    }

    @Test
    void shouldSuccessfullyLoadAdditionalPregnancyPaymentContext() {
        //Given
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        PaymentCycle latestPaymentCycle = aPaymentCycleWithClaim(claim);
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(paymentCycleRepository.findCurrentCycleForClaim(any())).willReturn(Optional.of(latestPaymentCycle));
        AdditionalPregnancyPaymentMessagePayload payload = AdditionalPregnancyPaymentMessagePayload.builder().claimId(claimId).build();
        given(payloadMapper.getPayload(any(), eq(AdditionalPregnancyPaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(ADDITIONAL_PREGNANCY_PAYMENT);

        //When
        AdditionalPregnancyPaymentMessageContext context = loader.loadAdditionalPregnancyPaymentMessageContext(message);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getPaymentCycle()).isEqualTo(Optional.of(latestPaymentCycle));
        verify(payloadMapper).getPayload(message, AdditionalPregnancyPaymentMessagePayload.class);
        verify(claimRepository).findById(claimId);
        verify(paymentCycleRepository).findCurrentCycleForClaim(claim);
    }

    @Test
    void shouldSuccessfullyLoadEmailMessageContext() {
        //Given
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        Message message = aValidMessageWithType(SEND_EMAIL);
        EmailMessagePayload payload = EmailMessagePayload.builder()
                .claimId(claimId)
                .emailType(EmailType.NEW_CARD)
                .emailPersonalisation(buildEmailPersonalisation())
                .build();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(payloadMapper.getPayload(message, EmailMessagePayload.class)).willReturn(payload);

        //When
        EmailMessageContext context = loader.loadEmailMessageContext(message);

        //Then
        assertThat(context.getTemplateId()).isEqualTo("bbbd8805-b020-41c9-b43f-c0e62318a6d5");
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getEmailPersonalisation()).isEqualTo(buildEmailPersonalisation());
        assertThat(context.getEmailType()).isEqualTo(EmailType.NEW_CARD);
        verify(payloadMapper).getPayload(message, EmailMessagePayload.class);
        verify(claimRepository).findById(claimId);
        verifyZeroInteractions(paymentCycleRepository);
    }

    @Test
    void shouldSuccessfullyLoadReportClaimMessageContext() {
        //Given
        Claim claim = aValidClaim();
        Message message = aValidMessageWithType(REPORT_CLAIM);
        ReportClaimMessagePayload payload = ReportClaimMessagePayload.builder()
                .claimId(claim.getId())
                .build();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(payloadMapper.getPayload(message, ReportClaimMessagePayload.class)).willReturn(payload);

        //When
        ReportClaimMessageContext context = loader.loadReportClaimMessageContext(message);

        //Then
        assertThat(context.getClaim()).isEqualTo(claim);
        verify(payloadMapper).getPayload(message, ReportClaimMessagePayload.class);
        verify(claimRepository).findById(claim.getId());
        verifyZeroInteractions(paymentCycleRepository);
    }
}
