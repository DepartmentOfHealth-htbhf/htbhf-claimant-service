package uk.gov.dhsc.htbhf.claimant.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.AttributeConverter;

/**
 * Converts a {@link List} of {@link LocalDate}s to json and visa versa.
 * Used to read/write values in the database.
 */
@Component
@AllArgsConstructor
public class ListOfLocalDatesConverter implements AttributeConverter<List<LocalDate>, String> {

    private static final TypeReference<List<LocalDate>> LIST_OF_LOCAL_DATES_TYPE = new TypeReference<>() {};

    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(List<LocalDate> dates) {
        try {
            return objectMapper.writeValueAsString(dates);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Unable to convert list of dates %s into a json string", dates.toString()), e);
        }
    }

    @Override
    public List<LocalDate> convertToEntityAttribute(String datesJson) {
        try {
            return objectMapper.readValue(datesJson, LIST_OF_LOCAL_DATES_TYPE);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to convert json string %s into a a list of local dates", datesJson), e);
        }
    }
}
