package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

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
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.CLAIMANT_ENDPOINT_URI;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertClaimantMatchesClaimantDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonDTOTestDataFactory.aValidPerson;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ClaimantServiceIntegrationTests {

    // Create a string 501 characters long
    private static final String LONG_STRING = CharBuffer.allocate(501).toString().replace('\0', 'A');
    private static final String ELIGIBILITY_SERVICE_URL = "http://localhost:8100/v1/eligibility";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ClaimantRepository claimantRepository;

    @MockBean
    RestTemplate restTemplateWithIdHeaders;

    @AfterEach
    void deleteAllClaimants() {
        claimantRepository.deleteAll();
    }

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
        assertClaimantPersistedSuccessfully(claim.getClaimant(), EligibilityStatus.ELIGIBLE);
        verify(restTemplateWithIdHeaders).postForEntity(ELIGIBILITY_SERVICE_URL, aValidPerson(), EligibilityResponse.class);
    }

    @Test
    void shouldFailWhenEligibilityServiceCallThrowsException() {
        //Given
        ClaimDTO claim = aValidClaimDTO();
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class))).willThrow(new RestClientException("Test exception"));
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildRequestEntity(claim), ErrorResponse.class);
        //Then
        ClaimantServiceAssertionUtils.assertErrorResponse(response, "An internal server error occurred", INTERNAL_SERVER_ERROR);
        assertClaimantPersistedSuccessfully(claim.getClaimant(), EligibilityStatus.ERROR);
        verify(restTemplateWithIdHeaders).postForEntity(ELIGIBILITY_SERVICE_URL, aValidPerson(), EligibilityResponse.class);
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

    //This is a MethodSource because of the use of LONG_STRING
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

    //This is a MethodSource because of the use of LONG_STRING
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

    @ParameterizedTest(name = "Field {0} with invalid value {1} on an error response")
    @CsvSource({
            "29-11-1909, dateOfBirth, '29-11-1909' could not be parsed as a LocalDate, claimant.dateOfBirth",
            "1999/12/31, dateOfBirth, '1999/12/31' could not be parsed as a LocalDate, claimant.dateOfBirth",
            "Foo, dateOfBirth, 'Foo' could not be parsed as a LocalDate, claimant.dateOfBirth",
            "29-11-1909, expectedDeliveryDate, '29-11-1909' could not be parsed as a LocalDate, claimant.expectedDeliveryDate",
            "1999/12/31, expectedDeliveryDate, '1999/12/31' could not be parsed as a LocalDate, claimant.expectedDeliveryDate",
            "Foo, expectedDeliveryDate, 'Foo' could not be parsed as a LocalDate, claimant.expectedDeliveryDate"
    })
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

    private void assertClaimantPersistedSuccessfully(ClaimantDTO claimantDTO, EligibilityStatus eligibilityStatus) {
        Iterable<Claimant> claims = claimantRepository.findAll();
        assertThat(claims).hasSize(1);
        Claimant persistedClaim = claims.iterator().next();
        assertClaimantMatchesClaimantDTO(claimantDTO, persistedClaim);
        assertThat(persistedClaim.getEligibilityStatus()).isEqualTo(eligibilityStatus);
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
        return new RequestEntity<>(requestObject, headers, HttpMethod.POST, CLAIMANT_ENDPOINT_URI);
    }

    private void assertValidationResponse(ResponseEntity<ErrorResponse> response, String expectedField, String expectedFieldMessage) {
        assertErrorResponse(response, expectedField, expectedFieldMessage, "There were validation issues with the request.");
    }

    private void assertErrorResponse(ResponseEntity<ErrorResponse> response, String expectedField, String expectedFieldMessage, String expectedErrorMessage) {
        ClaimantServiceAssertionUtils.assertErrorResponse(response, expectedErrorMessage, BAD_REQUEST);
        assertFieldErrors(response.getBody(), expectedField, expectedFieldMessage);
    }

    private void assertFieldErrors(ErrorResponse body, String expectedField, String expectedFieldMessage) {
        assertThat(body.getFieldErrors()).hasSize(1);
        ErrorResponse.FieldError fieldError = body.getFieldErrors().get(0);
        assertThat(fieldError.getField()).isEqualTo(expectedField);
        assertThat(fieldError.getMessage()).isEqualTo(expectedFieldMessage);
    }

}
