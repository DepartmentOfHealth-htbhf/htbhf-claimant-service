package uk.gov.dhsc.htbhf.claimant.entity.converter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ListOfLocalDatesConverterTest {

    @MockBean
    private ObjectMapper objectMapper;

    @Autowired
    private ListOfLocalDatesConverter converter;

    @Test
    void shouldConvertJsonStringToListOfLocalDates() throws IOException {
        String listJson = "[{\"2018-10-01\"}]";
        List<LocalDate> dates = singletonList(LocalDate.now());
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(dates);

        List<LocalDate> result = converter.convertToEntityAttribute(listJson);

        assertThat(result).isEqualTo(dates);
        ArgumentCaptor<TypeReference> argumentCaptor = ArgumentCaptor.forClass(TypeReference.class);
        verify(objectMapper).readValue(eq(listJson), argumentCaptor.capture());
        TypeReference typeReference = argumentCaptor.getValue();
        assertThat(typeReference.getType().getTypeName()).isEqualTo("java.util.List<java.time.LocalDate>");
    }

    @Test
    void shouldThrowExceptionWhenFailingToConvertJsonStringToListOfLocalDates() throws IOException {
        String listJson = "[{\"2018-10-01\"}]";
        JsonParseException jsonException = new JsonParseException(null, "json error");
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willThrow(jsonException);

        RuntimeException thrown = catchThrowableOfType(() -> converter.convertToEntityAttribute(listJson), RuntimeException.class);

        assertThat(thrown.getMessage()).isEqualTo("Unable to convert json string [{\"2018-10-01\"}] into a a list of local dates");
        assertThat(thrown.getCause()).isEqualTo(jsonException);
    }

    @Test
    void shouldConvertListOfLocalDatesToString() throws IOException {
        String listJson = "[{\"2018-10-01\"}]";
        List<LocalDate> dates = singletonList(LocalDate.now());
        given(objectMapper.writeValueAsString(any())).willReturn(listJson);

        String result = converter.convertToDatabaseColumn(dates);

        assertThat(result).isEqualTo(listJson);
        verify(objectMapper).writeValueAsString(dates);
    }

    @Test
    void shouldThrowExceptionWhenFailingToConvertListOfLocalDatesToJsonString() throws IOException {
        List<LocalDate> dates = singletonList(LocalDate.parse("2019-05-08"));
        JsonParseException jsonException = new JsonParseException(null, "json error");
        given(objectMapper.writeValueAsString(anyList())).willThrow(jsonException);

        RuntimeException thrown = catchThrowableOfType(() -> converter.convertToDatabaseColumn(dates), RuntimeException.class);

        assertThat(thrown.getMessage()).isEqualTo("Unable to convert list of dates [2019-05-08] into a json string");
        assertThat(thrown.getCause()).isEqualTo(jsonException);
    }
}
