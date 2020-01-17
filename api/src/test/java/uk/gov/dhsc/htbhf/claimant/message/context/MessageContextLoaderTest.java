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
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.*;
import static uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType.REGULAR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aCompleteNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aRequestNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aRequestPaymentPayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCalculationTestDataFactory.aFullPaymentCalculation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentResultTestDataFactory.aValidPaymentResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonalisationMapTestDataFactory.buildEmailPersonalisation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonalisationMapTestDataFactory.buildLetterPersonalisation;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

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
    void shouldSuccessfullyLoadRequestNewCardContext() {
        //Given
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        List<LocalDate> datesOfBirth = List.of(LocalDate.now().minusDays(1), LocalDate.now());
        RequestNewCardMessagePayload payload = aRequestNewCardMessagePayload(claimId, voucherEntitlement, datesOfBirth);
        given(payloadMapper.getPayload(any(), eq(RequestNewCardMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(REQUEST_NEW_CARD);

        //When
        RequestNewCardMessageContext context = loader.loadRequestNewCardContext(message);

        //Then
        assertThat(context).isNotNull();
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getEligibilityAndEntitlementDecision()).isEqualTo(payload.getEligibilityAndEntitlementDecision());
        verify(payloadMapper).getPayload(message, RequestNewCardMessagePayload.class);
        verify(claimRepository).findById(claimId);
    }

    @Test
    void shouldSuccessfullyLoadCompleteNewCardContext() {
        //Given
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        CompleteNewCardMessagePayload payload = aCompleteNewCardMessagePayload(claimId);
        given(payloadMapper.getPayload(any(), eq(CompleteNewCardMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(REQUEST_NEW_CARD);

        //When
        CompleteNewCardMessageContext context = loader.loadCompleteNewCardContext(message);

        //Then
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getEligibilityAndEntitlementDecision()).isEqualTo(payload.getEligibilityAndEntitlementDecision());
        assertThat(context.getCardAccountId()).isEqualTo(payload.getCardAccountId());
        verify(payloadMapper).getPayload(message, CompleteNewCardMessagePayload.class);
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
                .emailType(EmailType.INSTANT_SUCCESS)
                .emailPersonalisation(buildEmailPersonalisation())
                .build();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(payloadMapper.getPayload(message, EmailMessagePayload.class)).willReturn(payload);

        //When
        EmailMessageContext context = loader.loadEmailMessageContext(message);

        //Then
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getEmailPersonalisation()).isEqualTo(buildEmailPersonalisation());
        assertThat(context.getEmailType()).isEqualTo(EmailType.INSTANT_SUCCESS);
        verify(payloadMapper).getPayload(message, EmailMessagePayload.class);
        verify(claimRepository).findById(claimId);
        verifyNoInteractions(paymentCycleRepository);
    }

    @Test
    void shouldSuccessfullyLoadLetterMessageContext() {
        //Given
        Claim claim = aValidClaim();
        UUID claimId = claim.getId();
        Message message = aValidMessageWithType(SEND_LETTER);
        LetterMessagePayload payload = LetterMessagePayload.builder()
                .claimId(claimId)
                .letterType(LetterType.UPDATE_YOUR_ADDRESS)
                .personalisation(buildLetterPersonalisation())
                .build();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(payloadMapper.getPayload(message, LetterMessagePayload.class)).willReturn(payload);

        //When
        LetterMessageContext context = loader.loadLetterMessageContext(message);

        //Then
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getPersonalisation()).isEqualTo(buildLetterPersonalisation());
        assertThat(context.getLetterType()).isEqualTo(LetterType.UPDATE_YOUR_ADDRESS);
        verify(payloadMapper).getPayload(message, LetterMessagePayload.class);
        verify(claimRepository).findById(claimId);
        verifyNoInteractions(paymentCycleRepository);
    }

    @Test
    void shouldSuccessfullyLoadReportClaimMessageContext() {
        //Given
        Claim claim = aValidClaim();
        Message message = aValidMessageWithType(REPORT_CLAIM);
        ReportClaimMessagePayload payload = ReportClaimMessagePayload.builder()
                .claimId(claim.getId())
                .claimAction(ClaimAction.NEW)
                .timestamp(LocalDateTime.now())
                .identityAndEligibilityResponse(anIdMatchedEligibilityConfirmedUCResponseWithAllMatches())
                .updatedClaimantFields(List.of(FIRST_NAME))
                .build();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(payloadMapper.getPayload(message, ReportClaimMessagePayload.class)).willReturn(payload);

        //When
        ReportClaimMessageContext context = loader.loadReportClaimMessageContext(message);

        //Then
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getClaimAction()).isEqualTo(payload.getClaimAction());
        assertThat(context.getIdentityAndEligibilityResponse()).isEqualTo(payload.getIdentityAndEligibilityResponse());
        assertThat(context.getTimestamp()).isEqualTo(payload.getTimestamp());
        assertThat(context.getUpdatedClaimantFields()).isEqualTo(payload.getUpdatedClaimantFields());
        verify(payloadMapper).getPayload(message, ReportClaimMessagePayload.class);
        verify(claimRepository).findById(claim.getId());
        verifyNoInteractions(paymentCycleRepository);
    }

    @Test
    void shouldSuccessfullyLoadReportPaymentMessageContextWithAPaymentCycle() {
        //Given
        Claim claim = aValidClaim();
        Message message = aValidMessageWithType(REPORT_PAYMENT);
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);
        ReportPaymentMessagePayload payload = ReportPaymentMessagePayload.builder()
                .claimId(claim.getId())
                .paymentAction(PaymentAction.INITIAL_PAYMENT)
                .paymentCycleId(paymentCycle.getId())
                .identityAndEligibilityResponse(paymentCycle.getIdentityAndEligibilityResponse())
                .paymentForPregnancy(100)
                .paymentForChildrenUnderOne(100)
                .paymentForChildrenBetweenOneAndFour(100)
                .paymentForBackdatedVouchers(100)
                .timestamp(LocalDateTime.now())
                .build();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(paymentCycleRepository.findById(any())).willReturn(Optional.of(paymentCycle));
        given(payloadMapper.getPayload(message, ReportPaymentMessagePayload.class)).willReturn(payload);

        //When
        ReportPaymentMessageContext context = loader.loadReportPaymentMessageContext(message);

        //Then
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(context.getIdentityAndEligibilityResponse()).isEqualTo(payload.getIdentityAndEligibilityResponse());
        assertThat(context.getTimestamp()).isEqualTo(payload.getTimestamp());
        assertThat(context.getPaymentAction()).isEqualTo(payload.getPaymentAction());
        assertThat(context.getPaymentForPregnancy()).isEqualTo(100);
        assertThat(context.getPaymentForChildrenUnderOne()).isEqualTo(100);
        assertThat(context.getPaymentForChildrenBetweenOneAndFour()).isEqualTo(100);
        assertThat(context.getPaymentForBackdatedVouchers()).isEqualTo(100);
        verify(payloadMapper).getPayload(message, ReportPaymentMessagePayload.class);
        verify(claimRepository).findById(claim.getId());
        verify(paymentCycleRepository).findById(paymentCycle.getId());
    }

    @Test
    void shouldFailToLoadRequestPaymentContextIfClaimNotFound() {
        //Given
        UUID claimId = UUID.randomUUID();
        PaymentCycle paymentCycle = aValidPaymentCycle();
        UUID paymentCycleId = paymentCycle.getId();
        given(claimRepository.findById(any())).willReturn(Optional.empty());
        RequestPaymentMessagePayload payload = aRequestPaymentPayload(claimId, paymentCycleId, REGULAR_PAYMENT);
        given(payloadMapper.getPayload(any(), eq(RequestPaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(REQUEST_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadRequestPaymentMessageContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load claim using id: " + claimId);
        verify(claimRepository).findById(claimId);
        verify(payloadMapper).getPayload(message, RequestPaymentMessagePayload.class);
    }

    @Test
    void shouldFailToLoadRequestPaymentContextIfPaymentCycleNotFound() {
        //Given
        UUID paymentCycleId = UUID.randomUUID();
        Claim claim = aValidClaim();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(paymentCycleRepository.findById(any())).willReturn(Optional.empty());
        RequestPaymentMessagePayload payload = aRequestPaymentPayload(claim.getId(), paymentCycleId, REGULAR_PAYMENT);
        given(payloadMapper.getPayload(any(), eq(RequestPaymentMessagePayload.class))).willReturn(payload);
        Message message = aValidMessageWithType(REQUEST_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadRequestPaymentMessageContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to process message, unable to load payment cycle using id: " + paymentCycleId);
        verify(paymentCycleRepository).findById(paymentCycleId);
        verify(payloadMapper).getPayload(message, RequestPaymentMessagePayload.class);
    }

    @Test
    void shouldFailToLoadRequestPaymentContextIfUnableToMapPayload() {
        //Given
        MessageProcessingException testException = new MessageProcessingException("Unable to load payload");
        given(payloadMapper.getPayload(any(), eq(RequestPaymentMessagePayload.class))).willThrow(testException);
        Message message = aValidMessageWithType(REQUEST_PAYMENT);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> loader.loadRequestPaymentMessageContext(message), MessageProcessingException.class);

        //Then
        assertThat(thrown).isEqualTo(testException);
        verifyNoInteractions(paymentCycleRepository, claimRepository);
        verify(payloadMapper).getPayload(message, RequestPaymentMessagePayload.class);
    }

    @Test
    void shouldSuccessfullyLoadRequestPaymentMessageContext() {
        // Given
        Claim claim = aValidClaim();
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);
        PaymentType paymentType = REGULAR_PAYMENT;
        RequestPaymentMessagePayload payload = RequestPaymentMessagePayload.builder()
                .claimId(claim.getId())
                .paymentCycleId(paymentCycle.getId())
                .paymentType(paymentType)
                .build();
        Message message = aValidMessageWithType(REQUEST_PAYMENT);
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(paymentCycleRepository.findById(any())).willReturn(Optional.of(paymentCycle));
        given(payloadMapper.getPayload(message, RequestPaymentMessagePayload.class)).willReturn(payload);

        //When
        RequestPaymentMessageContext context = loader.loadRequestPaymentMessageContext(message);

        //Then
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(context.getPaymentType()).isEqualTo(paymentType);
        verify(payloadMapper).getPayload(message, RequestPaymentMessagePayload.class);
        verify(claimRepository).findById(claim.getId());
        verify(paymentCycleRepository).findById(paymentCycle.getId());
    }

    @Test
    void shouldSuccessfullyCompleteRequestPaymentMessageContext() {
        // Given
        Claim claim = aValidClaim();
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);
        PaymentCalculation paymentCalculation = aFullPaymentCalculation();
        PaymentType paymentType = REGULAR_PAYMENT;
        PaymentResult paymentResult = aValidPaymentResult();
        Message message = aValidMessageWithType(COMPLETE_PAYMENT);

        CompletePaymentMessagePayload payload = CompletePaymentMessagePayload.builder()
                .claimId(claim.getId())
                .paymentCycleId(paymentCycle.getId())
                .paymentCalculation(paymentCalculation)
                .paymentType(paymentType)
                .paymentResult(paymentResult)
                .build();
        given(claimRepository.findById(any())).willReturn(Optional.of(claim));
        given(paymentCycleRepository.findById(any())).willReturn(Optional.of(paymentCycle));
        given(payloadMapper.getPayload(message, CompletePaymentMessagePayload.class)).willReturn(payload);

        //When
        CompletePaymentMessageContext context = loader.loadCompletePaymentMessageContext(message);

        //Then
        assertThat(context.getClaim()).isEqualTo(claim);
        assertThat(context.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(context.getPaymentCalculation()).isEqualTo(paymentCalculation);
        assertThat(context.getPaymentType()).isEqualTo(paymentType);
        assertThat(context.getPaymentResult()).isEqualTo(paymentResult);
        verify(payloadMapper).getPayload(message, CompletePaymentMessagePayload.class);
        verify(claimRepository).findById(claim.getId());
        verify(paymentCycleRepository).findById(paymentCycle.getId());
    }
}
