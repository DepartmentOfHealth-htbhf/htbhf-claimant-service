package uk.gov.dhsc.htbhf.claimant.message;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aValidNewCardRequestMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.MESSAGE_PAYLOAD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;

@ExtendWith(MockitoExtension.class)
class PayloadMapperTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PayloadMapper payloadMapper;

    @Test
    void shouldThrownExceptionWhenMappingFails() throws IOException {
        //Given
        Message message = aValidMessage();
        JsonMappingException testException = new JsonMappingException(() -> { }, "Error reading value");
        given(objectMapper.readValue(anyString(), eq(NewCardRequestMessagePayload.class))).willThrow(testException);

        //When
        MessageProcessingException thrown = catchThrowableOfType(() -> payloadMapper.getPayload(message, NewCardRequestMessagePayload.class),
                MessageProcessingException.class);

        //Then
        assertThat(thrown).hasMessage("Unable to create message payload for message with id: %s, payload is: %s",
                message.getId(), MESSAGE_PAYLOAD);
        assertThat(thrown).hasCause(testException);
        verify(objectMapper).readValue(MESSAGE_PAYLOAD, NewCardRequestMessagePayload.class);
    }

    @Test
    void shouldSuccessfullyMapPayload() throws IOException {
        //Given
        Message message = aValidMessage();
        NewCardRequestMessagePayload payload = aValidNewCardRequestMessagePayload();
        given(objectMapper.readValue(anyString(), eq(NewCardRequestMessagePayload.class))).willReturn(payload);

        //When
        NewCardRequestMessagePayload newCardRequestMessagePayload = payloadMapper.getPayload(message, NewCardRequestMessagePayload.class);

        //Then
        assertThat(newCardRequestMessagePayload).isEqualTo(payload);
        verify(objectMapper).readValue(MESSAGE_PAYLOAD, NewCardRequestMessagePayload.class);
    }
}
