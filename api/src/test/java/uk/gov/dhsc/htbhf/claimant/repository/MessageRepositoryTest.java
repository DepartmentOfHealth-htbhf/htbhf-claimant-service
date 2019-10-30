package uk.gov.dhsc.htbhf.claimant.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aMessageWithMessageTimestamp;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @AfterEach
    void afterEach() {
        messageRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveAMessage() {
        //Given
        Message message = aValidMessage();
        messageRepository.save(message);
        //When
        Iterable<Message> allMessages = messageRepository.findAll();
        //Then
        Iterator<Message> messageIterator = allMessages.iterator();
        assertThat(messageIterator).containsOnly(message);
    }

    @Test
    void shouldRetrieveNoMessages() {
        //Given no message persisted
        //When
        Iterable<Message> allMessages = messageRepository.findAll();
        //Then
        Iterator<Message> messageIterator = allMessages.iterator();
        assertThat(messageIterator.hasNext()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "CREATE_NEW_CARD, MAKE_PAYMENT",
            "MAKE_PAYMENT, SEND_EMAIL",
            "SEND_EMAIL, CREATE_NEW_CARD"
    })
    void shouldRetrieveAllMessagesOfType(MessageType messageType, MessageType differentMessageType) {
        // Given
        Message newCardMessage = aValidMessageWithType(messageType);
        messageRepository.save(newCardMessage);
        messageRepository.save(aValidMessageWithType(differentMessageType));

        // When
        List<Message> messages = messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(messageType, Pageable.unpaged());

        // Then
        assertThat(messages).containsExactly(newCardMessage);
    }

    @Test
    void shouldRetrieveMessagesInChronologicalOrder() {
        // Given
        Message newCardMessage = aMessageWithMessageTimestamp(LocalDateTime.now());
        Message newCardMessageOneMonthAgo = aMessageWithMessageTimestamp(LocalDateTime.now().minusMonths(1));
        Message newCardMessageOneYearAgo = aMessageWithMessageTimestamp(LocalDateTime.now().minusYears(1));
        messageRepository.save(newCardMessageOneMonthAgo);
        messageRepository.save(newCardMessage);
        messageRepository.save(newCardMessageOneYearAgo);

        // When
        List<Message> messages = messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(MessageType.CREATE_NEW_CARD, Pageable.unpaged());

        // Then
        assertThat(messages).containsExactly(newCardMessageOneYearAgo, newCardMessageOneMonthAgo, newCardMessage);
    }

    @Test
    void shouldExcludeMessageWithTimestampInFuture() {
        // Given
        Message newCardMessageInFuture = aMessageWithMessageTimestamp(LocalDateTime.now().plusMinutes(1));
        Message newCardMessageOneMonthAgo = aMessageWithMessageTimestamp(LocalDateTime.now().minusMonths(1));
        Message newCardMessageOneYearAgo = aMessageWithMessageTimestamp(LocalDateTime.now().minusYears(1));
        messageRepository.save(newCardMessageInFuture);
        messageRepository.save(newCardMessageOneMonthAgo);
        messageRepository.save(newCardMessageOneYearAgo);

        // When
        List<Message> messages = messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(MessageType.CREATE_NEW_CARD, Pageable.unpaged());

        // Then
        assertThat(messages).containsExactly(newCardMessageOneYearAgo, newCardMessageOneMonthAgo);
    }

    @Test
    void shouldRetrieveMessagesUsingPageSizeOnly() {
        // Given
        Message message1 = aValidMessage();
        Message message2 = aValidMessage();
        Message message3 = aValidMessage();
        messageRepository.save(message1);
        messageRepository.save(message2);
        messageRepository.save(message3);

        // When
        List<Message> messages = messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(MessageType.CREATE_NEW_CARD, PageRequest.of(0, 2));

        // Then
        assertThat(messages).containsExactly(message1, message2);
    }

}
