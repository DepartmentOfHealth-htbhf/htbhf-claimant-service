package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.TextMessageContext;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendSmsResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_MOBILE;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_TEXT;
import static uk.gov.dhsc.htbhf.claimant.message.payload.TextType.INSTANT_SUCCESS_TEXT;
import static uk.gov.dhsc.htbhf.claimant.service.audit.FailedEventTestUtils.verifySendTextEventFailExceptionAndEventAreCorrect;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonalisationMapTestDataFactory.buildTextPersonalisation;
import static uk.gov.dhsc.htbhf.claimant.utilities.TestLoggingUtilities.startRecordingLogsFor;
import static uk.gov.dhsc.htbhf.claimant.utilities.TestLoggingUtilities.stopRecordingLogsFor;

@ExtendWith(MockitoExtension.class)
class TextMessageProcessorTest {

    @Mock
    private NotificationClient client;
    @Mock
    private MessageContextLoader messageContextLoader;

    private TextMessageProcessor textMessageProcessor;

    @BeforeEach
    void init() {
        startRecordingLogsFor(TextMessageProcessor.class);
        textMessageProcessor = new TextMessageProcessor(client, messageContextLoader);
    }

    @AfterEach
    void tearDown() {
        stopRecordingLogsFor(TextMessageProcessor.class);
    }

    @Test
    void shouldSendTextMessage() throws NotificationClientException {
        //Given
        Claim claim = aValidClaim();
        Map<String, Object> textPersonalisation = buildTextPersonalisation();
        TextMessageContext context = TextMessageContext.builder()
                .claim(claim)
                .textPersonalisation(textPersonalisation)
                .textType(INSTANT_SUCCESS_TEXT)
                .build();
        given(messageContextLoader.loadTextMessageContext(any())).willReturn(context);
        given(client.sendSms(any(), any(), any(), any())).willReturn(mock(SendSmsResponse.class));
        Message message = aValidMessageWithType(SEND_TEXT);

        //When
        MessageStatus status = textMessageProcessor.processMessage(message);

        //Then
        assertThat(status).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadTextMessageContext(message);
        verify(client).sendSms(eq(INSTANT_SUCCESS_TEXT.getTemplateId()), eq(HOMER_MOBILE), eq(textPersonalisation), any(String.class));
    }

    @Test
    void shouldNotSendTextMessageForMissingPhone() {
        //Given
        Claim claim = aValidClaim();
        claim.getClaimant().setPhoneNumber(null);
        Map<String, Object> textPersonalisation = buildTextPersonalisation();
        TextMessageContext context = TextMessageContext.builder()
                .claim(claim)
                .textPersonalisation(textPersonalisation)
                .textType(INSTANT_SUCCESS_TEXT)
                .build();
        given(messageContextLoader.loadTextMessageContext(any())).willReturn(context);
        Message message = aValidMessageWithType(SEND_TEXT);

        //When
        textMessageProcessor.processMessage(message);

        //Then
        verifyNoInteractions(client);
    }

    @Test
    void shouldThrowFailedEventExceptionWhenSendTextMessageFails() throws NotificationClientException {
        //Given
        Claim claim = aValidClaim();
        Map<String, Object> textPersonalisation = buildTextPersonalisation();
        TextMessageContext context = TextMessageContext.builder()
                .claim(claim)
                .textPersonalisation(textPersonalisation)
                .textType(INSTANT_SUCCESS_TEXT)
                .build();
        given(messageContextLoader.loadTextMessageContext(any())).willReturn(context);
        Message message = aValidMessageWithType(SEND_TEXT);
        NotificationClientException testException = new NotificationClientException("Test exception from message send");
        given(client.sendSms(anyString(), anyString(), any(), anyString()))
                .willThrow(testException);

        //When
        EventFailedException thrown = catchThrowableOfType(() -> textMessageProcessor.processMessage(message), EventFailedException.class);

        //Then
        verifySendTextEventFailExceptionAndEventAreCorrect(claim, testException, thrown, INSTANT_SUCCESS_TEXT.getTemplateId());
        verify(messageContextLoader).loadTextMessageContext(message);
        verify(client).sendSms(eq(INSTANT_SUCCESS_TEXT.getTemplateId()), eq(HOMER_MOBILE), eq(textPersonalisation), any(String.class));
    }

}
