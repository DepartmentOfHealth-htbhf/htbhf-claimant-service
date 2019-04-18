package uk.gov.dhsc.htbhf.claimant.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Message;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;

@SpringBootTest
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
        assertThat(messageIterator.hasNext()).isTrue();
        Message foundMessage = messageIterator.next();
        assertThat(foundMessage).isEqualTo(message);
        assertThat(messageIterator.hasNext()).isFalse();
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

}
