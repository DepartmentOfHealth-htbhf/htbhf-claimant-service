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

    private static final String LIST_OF_DATES_JSON = "[{\"2018-10-01\"}]";
    private static final List<LocalDate> LIST_OF_DATES = singletonList(LocalDate.parse("2018-10-01"));

    @MockBean
    private ObjectMapper objectMapper;

    @Autowired
    private ListOfLocalDatesConverter converter;

    @Test
    void shouldConvertJsonStringToListOfLocalDates() throws IOException {
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(LIST_OF_DATES);

        List<LocalDate> result = converter.convertToEntityAttribute(LIST_OF_DATES_JSON);

        assertThat(result).isEqualTo(LIST_OF_DATES);
        ArgumentCaptor<TypeReference> argumentCaptor = ArgumentCaptor.forClass(TypeReference.class);
        verify(objectMapper).readValue(eq(LIST_OF_DATES_JSON), argumentCaptor.capture());
        TypeReference typeReference = argumentCaptor.getValue();
        assertThat(typeReference.getType().getTypeName()).isEqualTo("java.util.List<java.time.LocalDate>");
    }

    @Test
    void shouldThrowExceptionWhenFailingToConvertJsonStringToListOfLocalDates() throws IOException {
        JsonParseException jsonException = new JsonParseException(null, "json error");
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willThrow(jsonException);

        RuntimeException thrown = catchThrowableOfType(() -> converter.convertToEntityAttribute(LIST_OF_DATES_JSON), RuntimeException.class);

        assertThat(thrown.getMessage()).isEqualTo("Unable to convert json string [{\"2018-10-01\"}] into a list of local dates");
        assertThat(thrown.getCause()).isEqualTo(jsonException);
    }

    @Test
    void shouldConvertListOfLocalDatesToString() throws IOException {
        given(objectMapper.writeValueAsString(any())).willReturn(LIST_OF_DATES_JSON);

        String result = converter.convertToDatabaseColumn(LIST_OF_DATES);

        assertThat(result).isEqualTo(LIST_OF_DATES_JSON);
        verify(objectMapper).writeValueAsString(LIST_OF_DATES);
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
