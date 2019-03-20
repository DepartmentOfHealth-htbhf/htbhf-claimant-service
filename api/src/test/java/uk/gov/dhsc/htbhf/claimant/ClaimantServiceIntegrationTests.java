package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.claimant.controller.ErrorResponse;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;

import java.nio.CharBuffer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonDTOTestDataFactory.aValidPerson;

public class ClaimantServiceIntegrationTests extends AbstractIntegrationTest {

    // Create a string 501 characters long
    private static final String LONG_STRING = CharBuffer.allocate(501).toString().replace('\0', 'A');

    @Test
    void shouldAcceptAndCreateANewValidClaimWithNoNullFields() {
        shouldAcceptAndCreateValidClaim(aValidClaimDTOWithNoNullFields());
    }

    @Test
    void shouldAcceptAndCreateANewValidClaimWithNullFields() {
        shouldAcceptAndCreateValidClaim(aValidClaimDTO());
    }

    private void shouldAcceptAndCreateValidClaim(ClaimDTO claim) {
        //Given
        ResponseEntity<EligibilityResponse> eligibilityResponse = new ResponseEntity<>(anEligibilityResponse(), HttpStatus.OK);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class))).willReturn(eligibilityResponse);
        //When
        ResponseEntity<Void> response = restTemplate.exchange(buildRequestEntity(claim), Void.class);
        //Then
        assertThat(response.getStatusCode()).isEqualTo(CREATED);

        Iterable<Claimant> claims = claimantRepository.findAll();
        assertThat(claims).hasSize(1);
        Claimant persistedClaim = claims.iterator().next();
        assertClaimantMatchesClaimantDTO(claim.getClaimant(), persistedClaim);
        verify(restTemplateWithIdHeaders).postForEntity("http://localhost:8100/v1/eligibility", aValidPerson(), EligibilityResponse.class);
        claimantRepository.deleteAll();
    }

    @Test
    void shouldReturn404ErrorForNonExistentPath() {
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/missing-resource", ErrorResponse.class);
        //Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @ParameterizedTest(name = "Field {0} with invalid value {1} on a claim returns the correct error response")
    @MethodSource("shouldFailWithValidationErrorArguments")
    void shouldFailWithValidationError(String fieldName, String value, String expectedErrorMessage, String expectedField) {
        //Given
        ClaimDTO claim = aValidClaimDTO();
        modifyFieldOnObject(claim.getClaimant(), fieldName, value);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationResponse(response, expectedField, expectedErrorMessage);
    }

    @ParameterizedTest(name = "Field {0} with invalid value {1} on an address returns the correct error response")
    @MethodSource("shouldFailWithAddressValidationErrorArguments")
    void shouldFailWithAddressValidationError(String fieldName, String value, String expectedErrorMessage, String expectedField) {
        //Given
        ClaimDTO claim = aValidClaimDTO();
        modifyFieldOnObject(claim.getClaimant().getCardDeliveryAddress(), fieldName, value);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationResponse(response, expectedField, expectedErrorMessage);
    }

    @ParameterizedTest(name = "Field {0} with invalid value {1} on an error response")
    @MethodSource("shouldFailWithInvalidDateFormatErrorArguments")
    void shouldFailWithInvalidDateFormatError(String dateString, String fieldName, String expectedErrorMessage, String expectedField)
            throws JsonProcessingException, JSONException {
        //Given
        String claimWithInvalidDate = modifyFieldOnClaimantInJson(aValidClaimDTO(), fieldName, dateString);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildRequestEntity(claimWithInvalidDate), ErrorResponse.class);
        //Then
        assertErrorResponse(response, expectedField, expectedErrorMessage, "The request could not be parsed.");
    }

    @Test
    void shouldReturnErrorGivenAnEmptyClaim() {
        //Given
        String claim = "{}";
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationResponse(response, "claimant", "must not be null");
    }

    private static Stream<Arguments> shouldFailWithInvalidDateFormatErrorArguments() {
        return Stream.of(
                Arguments.of("29-11-1909", "dateOfBirth", "'29-11-1909' could not be parsed as a LocalDate", "claimant.dateOfBirth"),
                Arguments.of("1999/12/31", "dateOfBirth", "'1999/12/31' could not be parsed as a LocalDate", "claimant.dateOfBirth"),
                Arguments.of("Foo", "dateOfBirth", "'Foo' could not be parsed as a LocalDate", "claimant.dateOfBirth"),
                Arguments.of("29-11-1909", "expectedDeliveryDate", "'29-11-1909' could not be parsed as a LocalDate", "claimant.expectedDeliveryDate"),
                Arguments.of("1999/12/31", "expectedDeliveryDate", "'1999/12/31' could not be parsed as a LocalDate", "claimant.expectedDeliveryDate"),
                Arguments.of("Foo", "expectedDeliveryDate", "'Foo' could not be parsed as a LocalDate", "claimant.expectedDeliveryDate")
        );
    }

    private static Stream<Arguments> shouldFailWithAddressValidationErrorArguments() {
        return Stream.of(
                Arguments.of("addressLine1", null, "must not be null", "claimant.cardDeliveryAddress.addressLine1"),
                Arguments.of("addressLine1", LONG_STRING, "size must be between 1 and 500", "claimant.cardDeliveryAddress.addressLine1"),
                Arguments.of("addressLine2", LONG_STRING, "size must be between 0 and 500", "claimant.cardDeliveryAddress.addressLine2"),
                Arguments.of("townOrCity", null, "must not be null", "claimant.cardDeliveryAddress.townOrCity"),
                Arguments.of("townOrCity", LONG_STRING, "size must be between 1 and 500", "claimant.cardDeliveryAddress.townOrCity"),
                Arguments.of("postcode", "AA1122BB", "invalid postcode format", "claimant.cardDeliveryAddress.postcode"),
                Arguments.of("postcode", "A", "invalid postcode format", "claimant.cardDeliveryAddress.postcode"),
                Arguments.of("postcode", "11AA21", "invalid postcode format", "claimant.cardDeliveryAddress.postcode"),
                Arguments.of("postcode", "", "invalid postcode format", "claimant.cardDeliveryAddress.postcode"),
                Arguments.of("postcode", null, "must not be null", "claimant.cardDeliveryAddress.postcode")
        );
    }

    private static Stream<Arguments> shouldFailWithValidationErrorArguments() {
        return Stream.of(
                Arguments.of("lastName", LONG_STRING, "size must be between 1 and 500", "claimant.lastName"),
                Arguments.of("lastName", null, "must not be null", "claimant.lastName"),
                Arguments.of("lastName", "", "size must be between 1 and 500", "claimant.lastName"),
                Arguments.of("firstName", LONG_STRING, "size must be between 0 and 500", "claimant.firstName"),
                Arguments.of("nino", null, "must not be null", "claimant.nino"),
                Arguments.of("nino", "", "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"", "claimant.nino"),
                Arguments.of("nino", "YYHU456781", "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"", "claimant.nino"),
                Arguments.of("nino", "888888888", "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"", "claimant.nino"),
                Arguments.of("nino", "ABCDEFGHI", "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"", "claimant.nino"),
                Arguments.of("nino", "ZQQ123456CZ", "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"", "claimant.nino"),
                Arguments.of("nino", "QQ123456T", "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"", "claimant.nino"),
                Arguments.of("dateOfBirth", "9999-12-31", "must be a past date", "claimant.dateOfBirth"),
                Arguments.of("expectedDeliveryDate", "9999-12-31",
                        "must not be more than one month in the past or 8 months in the future", "claimant.expectedDeliveryDate"),
                Arguments.of("expectedDeliveryDate", "1990-12-31",
                        "must not be more than one month in the past or 8 months in the future", "claimant.expectedDeliveryDate"),
                Arguments.of("cardDeliveryAddress", null, "must not be null", "claimant.cardDeliveryAddress"),
                Arguments.of("dateOfBirth", null, "must not be null", "claimant.dateOfBirth")
        );
    }

    private String modifyFieldOnClaimantInJson(Object originalValue, String fieldName, String newValue) throws JsonProcessingException, JSONException {
        String json = objectMapper.writeValueAsString(originalValue);
        JSONObject jsonObject = new JSONObject(json);
        jsonObject.getJSONObject("claimant").put(fieldName, newValue);
        return jsonObject.toString();
    }

    private void modifyFieldOnObject(Object nestedObjectToSetFieldOn, String fieldName, String value) {
        Object valueToSet = (isLocalDateField(fieldName) && value != null) ? LocalDate.parse(value) : value;
        ReflectionTestUtils.setField(nestedObjectToSetFieldOn, fieldName, valueToSet);
    }

    private boolean isLocalDateField(String fieldName) {
        List dateFields = Arrays.asList("dateOfBirth", "expectedDeliveryDate");
        return dateFields.contains(fieldName);
    }

    private RequestEntity buildRequestEntity(Object requestObject) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new RequestEntity<>(requestObject, headers, HttpMethod.POST, endpointUrl);
    }

    private void assertValidationResponse(ResponseEntity<ErrorResponse> response, String expectedField, String expectedFieldMessage) {
        assertErrorResponse(response, expectedField, expectedFieldMessage, "There were validation issues with the request.");
    }

    private void assertErrorResponse(ResponseEntity<ErrorResponse> response, String expectedField, String expectedFieldMessage, String expectedErrorMessage) {
        super.assertErrorResponse(response, expectedErrorMessage, BAD_REQUEST);
        assertFieldErrors(response.getBody(), expectedField, expectedFieldMessage);
    }

    private void assertFieldErrors(ErrorResponse body, String expectedField, String expectedFieldMessage) {
        assertThat(body.getFieldErrors()).hasSize(1);
        ErrorResponse.FieldError fieldError = body.getFieldErrors().get(0);
        assertThat(fieldError.getField()).isEqualTo(expectedField);
        assertThat(fieldError.getMessage()).isEqualTo(expectedFieldMessage);
    }

}
