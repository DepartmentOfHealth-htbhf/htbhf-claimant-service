package uk.gov.dhsc.htbhf.claimant.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import java.io.IOException;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.FAILED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aValidNewCardRequestMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.MESSAGE_PAYLOAD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithPayload;

@SpringBootTest
@Transactional
class NewCardMessageProcessorTest {

    @MockBean
    private NewCardService newCardService;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private MessageRepository messageRepository;

    @Autowired
    private NewCardMessageProcessor newCardMessageProcessor;

    @Test
    void shouldRollBackTransactionAndReturnFailedWhenExceptionIsThrown() throws IOException {
        given(objectMapper.readValue(anyString(), eq(NewCardRequestMessagePayload.class))).willThrow(new IOException("Error reading value"));

        MessageStatus status = newCardMessageProcessor.processMessage(aValidMessageWithPayload(MESSAGE_PAYLOAD));

        assertThat(status).isEqualTo(FAILED);
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
        verify(objectMapper).readValue(MESSAGE_PAYLOAD, NewCardRequestMessagePayload.class);
    }

    @Test
    void shouldCreateNewCardAndDeleteMessage() throws IOException {
        NewCardRequestMessagePayload payload = aValidNewCardRequestMessagePayload();
        given(objectMapper.readValue(anyString(), eq(NewCardRequestMessagePayload.class))).willReturn(payload);
        Message entity = aValidMessage();

        MessageStatus status = newCardMessageProcessor.processMessage(entity);

        assertThat(status).isEqualTo(COMPLETED);
        assertThat(TestTransaction.isActive()).isTrue();
        verify(newCardService).createNewCard(payload.getClaimId());
        verify(messageRepository).delete(entity);
    }
}
