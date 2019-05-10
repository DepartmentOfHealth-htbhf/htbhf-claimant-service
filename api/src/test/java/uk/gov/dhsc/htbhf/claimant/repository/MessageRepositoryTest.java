package uk.gov.dhsc.htbhf.claimant.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithTimestamp;
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
            "CREATE_NEW_CARD, MAKE_FIRST_PAYMENT",
            "MAKE_FIRST_PAYMENT, SEND_FIRST_EMAIL",
            "SEND_FIRST_EMAIL, CREATE_NEW_CARD"
    })
    void shouldRetrieveAllMessagesOfType(MessageType messageType, MessageType differentMessageType) {
        // Given
        Message newCardMessage = aValidMessageWithType(messageType);
        messageRepository.save(newCardMessage);
        messageRepository.save(aValidMessageWithType(differentMessageType));

        // When
        List<Message> messages = messageRepository.findAllMessagesByTypeOrderedByDate(messageType);

        // Then
        assertThat(messages).containsExactly(newCardMessage);
    }

    @Test
    void shouldRetrieveMessagesInChronologicalOrder() {
        // Given
        Message newCardMessage = aValidMessageWithTimestamp(LocalDateTime.now());
        Message newCardMessageOneMonthAgo = aValidMessageWithTimestamp(LocalDateTime.now().minusMonths(1));
        Message newCardMessageOneYearAgo = aValidMessageWithTimestamp(LocalDateTime.now().minusYears(1));
        messageRepository.save(newCardMessageOneMonthAgo);
        messageRepository.save(newCardMessage);
        messageRepository.save(newCardMessageOneYearAgo);

        // When
        List<Message> messages = messageRepository.findAllMessagesByTypeOrderedByDate(MessageType.CREATE_NEW_CARD);

        // Then
        assertThat(messages).containsExactly(newCardMessageOneYearAgo, newCardMessageOneMonthAgo, newCardMessage);
    }

}
