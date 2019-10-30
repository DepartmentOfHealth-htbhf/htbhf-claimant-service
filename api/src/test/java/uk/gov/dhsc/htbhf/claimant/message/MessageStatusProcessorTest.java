package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.ERROR;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aMessageWithMessageTimestamp;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aMessageWithMessageTimestampAndDeliveryCount;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;

@ExtendWith(MockitoExtension.class)
class MessageStatusProcessorTest {

    private static final long TWELVE_HOURS_IN_SECONDS = 60L * 60 * 12;

    @Mock
    private MessageRepository messageRepository;

    @Captor
    private ArgumentCaptor<Message> captor;

    private MessageStatusProcessor messageStatusProcessor;

    @BeforeEach
    void setup() {
        messageStatusProcessor = new MessageStatusProcessor(messageRepository, TWELVE_HOURS_IN_SECONDS);
    }

    @Test
    void shouldDeleteMessageWhenCompleted() {
        //Given
        Message message = aValidMessage();
        //When
        messageStatusProcessor.processStatusForMessage(message, COMPLETED);
        //Then
        verify(messageRepository).delete(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAILED", "ERROR"})
    void shouldUpdateMessageWhenNotCompleted(MessageStatus messageStatus) {
        //Given
        LocalDateTime originalTimestamp = LocalDateTime.now().minusHours(1);
        Message message = aMessageWithMessageTimestamp(originalTimestamp);
        //When
        messageStatusProcessor.processStatusForMessage(message, messageStatus);
        //Then
        verify(messageRepository).save(captor.capture());
        assertMessageUpdated(captor.getValue(), message.getId(), originalTimestamp, messageStatus);
    }

    @Test
    void shouldUpdateMessagesToErrorAndIncrementCount() {
        //Given
        LocalDateTime originalTimestamp1 = LocalDateTime.now().minusHours(1);
        Message message1 = aMessageWithMessageTimestamp(originalTimestamp1);
        LocalDateTime originalTimestamp2 = LocalDateTime.now().minusHours(2);
        Message message2 = aMessageWithMessageTimestamp(originalTimestamp2);
        LocalDateTime originalTimestamp3 = LocalDateTime.now().minusHours(3);
        Message message3 = aMessageWithMessageTimestamp(originalTimestamp3);
        List<Message> messages = List.of(message1, message2, message3);
        //When
        messageStatusProcessor.updateMessagesToErrorAndIncrementCount(messages);
        //Then
        verify(messageRepository, times(3)).save(captor.capture());
        List<Message> savedMessages = captor.getAllValues();
        assertThat(savedMessages).hasSize(3);
        assertMessageUpdated(savedMessages.get(0), message1.getId(), originalTimestamp1, ERROR);
        assertMessageUpdated(savedMessages.get(1), message2.getId(), originalTimestamp2, ERROR);
        assertMessageUpdated(savedMessages.get(2), message3.getId(), originalTimestamp3, ERROR);
    }

    @ParameterizedTest(name = "delivery count of {0} has delay of {1}")
    @CsvSource({
            "0, PT30S",
            "1, PT1M",
            "2, PT2M",
            "3, PT4M",
            "4, PT8M",
            "5, PT16M",
            "6, PT32M",
            "7, PT64M",
            "8, PT128M",
            "10, PT512M",
    })
    void shouldExponentiallyIncreaseTheTimestampOfFailedMessages(int initialDeliveryCount, String timestampIncrement) {
        //Given
        final Duration expectedDelay = Duration.parse(timestampIncrement);
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime expectedTimestampStart = now.plus(expectedDelay);
        Message message = aMessageWithMessageTimestampAndDeliveryCount(now, initialDeliveryCount);
        //When
        messageStatusProcessor.processStatusForMessage(message, MessageStatus.FAILED);
        //Then
        verify(messageRepository).save(captor.capture());
        Message savedMessage = captor.getValue();
        assertThat(savedMessage.getDeliveryCount()).isEqualTo(initialDeliveryCount + 1);
        assertThat(savedMessage.getMessageTimestamp()).isBetween(expectedTimestampStart, expectedTimestampStart.plusSeconds(1));
    }

    @Test
    void shouldNotIncreaseTheTimestampPastMaximumRetryPeriod() {
        //Given
        LocalDateTime originalTimestamp = LocalDateTime.now();
        Message message = aMessageWithMessageTimestampAndDeliveryCount(originalTimestamp, 1000);
        final LocalDateTime expectedTimestampStart = LocalDateTime.now().plusSeconds(TWELVE_HOURS_IN_SECONDS);
        //When
        messageStatusProcessor.processStatusForMessage(message, MessageStatus.FAILED);
        //Then
        verify(messageRepository).save(captor.capture());
        Message savedMessage = captor.getValue();
        assertThat(savedMessage.getDeliveryCount()).isEqualTo(1001);
        assertThat(savedMessage.getMessageTimestamp()).isBetween(expectedTimestampStart, expectedTimestampStart.plusSeconds(1));
    }

    private void assertMessageUpdated(Message savedMessage, UUID messageId, LocalDateTime originalTimestamp, MessageStatus messageStatus) {
        assertThat(savedMessage.getDeliveryCount()).isEqualTo(1);
        assertThat(savedMessage.getId()).isEqualTo(messageId);
        assertThat(savedMessage.getMessageTimestamp()).isAfter(originalTimestamp);
        assertThat(savedMessage.getStatus()).isEqualTo(messageStatus);
    }
}
