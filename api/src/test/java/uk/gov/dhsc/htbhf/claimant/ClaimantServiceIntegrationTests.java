package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
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
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
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
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertInternalServerErrorResponse;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertRequestCouldNotBeParsedErrorResponse;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertValidationErrorInResponse;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.CLAIMANT_ENDPOINT_URI;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertClaimantMatchesClaimantDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantInSameHouseholdBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithDwpHouseholdIdentifier;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithHmrcHouseholdIdentifier;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonDTOTestDataFactory.aValidPerson;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ERROR;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
class ClaimantServiceIntegrationTests {

    // Create a string 501 characters long
    private static final String LONG_STRING = CharBuffer.allocate(501).toString().replace('\0', 'A');
    private static final String ELIGIBILITY_SERVICE_URL = "http://localhost:8100/v1/eligibility";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ClaimRepository claimRepository;

    @Autowired
    MessageRepository messageRepository;

    @MockBean
    RestTemplate restTemplateWithIdHeaders;

    @AfterEach
    void deleteAllClaimsAndMessages() {
        claimRepository.deleteAll();
        messageRepository.deleteAll();
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
        ResponseEntity<EligibilityResponse> eligibilityResponse = new ResponseEntity<>(anEligibilityResponseWithStatus(ELIGIBLE), HttpStatus.OK);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class))).willReturn(eligibilityResponse);
        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildRequestEntity(claim), ClaimResultDTO.class);
        //Then
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(response.getBody().getVoucherEntitlement()).isEqualTo(aValidVoucherEntitlementDTO());
        assertClaimPersistedSuccessfully(claim, ELIGIBLE, "dwpHousehold1", "hmrcHousehold1");
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
        assertInternalServerErrorResponse(response);
        assertClaimPersistedSuccessfully(claim, ERROR, null, null);
        verify(restTemplateWithIdHeaders).postForEntity(ELIGIBILITY_SERVICE_URL, aValidPerson(), EligibilityResponse.class);
    }

    @Test
    void shouldReturn404ErrorForNonExistentPath() {
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/missing-resource", ErrorResponse.class);
        //Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    // TODO: MGS: add test to confirm existing claim is updated and response includes CLAIM_UPDATED status. HTBHF-1192

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsForDwpHouseholdIdentifier() {
        //Given
        String householdIdentifier = "dwpHousehold1";
        ClaimDTO dto = aValidClaimDTO();
        Claimant claimant = aValidClaimantInSameHouseholdBuilder().build();
        Claim claim = aValidClaimBuilder()
                .dwpHouseholdIdentifier(householdIdentifier)
                .claimant(claimant)
                .build();
        claimRepository.save(claim);

        EligibilityResponse eligibilityResponse = anEligibilityResponseWithDwpHouseholdIdentifier(householdIdentifier);
        ResponseEntity<EligibilityResponse> eligibilityResponseEntity = new ResponseEntity<>(eligibilityResponse, HttpStatus.OK);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class))).willReturn(eligibilityResponseEntity);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildRequestEntity(dto), ClaimResultDTO.class);

        //Then
        assertDuplicateResponse(response);
        verify(restTemplateWithIdHeaders).postForEntity(ELIGIBILITY_SERVICE_URL, aValidPerson(), EligibilityResponse.class);
    }

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsForHmrcHouseholdIdentifier() {
        //Given
        String householdIdentifier = "hmrcHousehold1";
        ClaimDTO dto = aValidClaimDTO();
        Claimant claimant = aValidClaimantInSameHouseholdBuilder().build();
        Claim claim = aValidClaimBuilder()
                .hmrcHouseholdIdentifier(householdIdentifier)
                .claimant(claimant)
                .build();
        claimRepository.save(claim);

        EligibilityResponse eligibilityResponse = anEligibilityResponseWithHmrcHouseholdIdentifier(householdIdentifier);
        ResponseEntity<EligibilityResponse> eligibilityResponseEntity = new ResponseEntity<>(eligibilityResponse, HttpStatus.OK);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class))).willReturn(eligibilityResponseEntity);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildRequestEntity(dto), ClaimResultDTO.class);

        //Then
        assertDuplicateResponse(response);
        verify(restTemplateWithIdHeaders).postForEntity(ELIGIBILITY_SERVICE_URL, aValidPerson(), EligibilityResponse.class);
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
        assertValidationErrorInResponse(response, expectedField, expectedErrorMessage);
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
                Arguments.of("address", null, "must not be null", "claimant.address"),
                Arguments.of("dateOfBirth", null, "must not be null", "claimant.dateOfBirth")
        );
    }

    @ParameterizedTest(name = "Field {0} with invalid value {1} on an address returns the correct error response")
    @MethodSource("shouldFailWithAddressValidationErrorArguments")
    void shouldFailWithAddressValidationError(String fieldName, String value, String expectedErrorMessage, String expectedField) {
        //Given
        ClaimDTO claim = aValidClaimDTO();
        modifyFieldOnObject(claim.getClaimant().getAddress(), fieldName, value);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, expectedField, expectedErrorMessage);
    }

    //This is a MethodSource because of the use of LONG_STRING
    private static Stream<Arguments> shouldFailWithAddressValidationErrorArguments() {
        return Stream.of(
                Arguments.of("addressLine1", null, "must not be null", "claimant.address.addressLine1"),
                Arguments.of("addressLine1", LONG_STRING, "size must be between 1 and 500", "claimant.address.addressLine1"),
                Arguments.of("addressLine2", LONG_STRING, "size must be between 0 and 500", "claimant.address.addressLine2"),
                Arguments.of("townOrCity", null, "must not be null", "claimant.address.townOrCity"),
                Arguments.of("townOrCity", LONG_STRING, "size must be between 1 and 500", "claimant.address.townOrCity"),
                Arguments.of("postcode", "AA1122BB", "invalid postcode format", "claimant.address.postcode"),
                Arguments.of("postcode", "A", "invalid postcode format", "claimant.address.postcode"),
                Arguments.of("postcode", "11AA21", "invalid postcode format", "claimant.address.postcode"),
                Arguments.of("postcode", "", "invalid postcode format", "claimant.address.postcode"),
                Arguments.of("postcode", null, "must not be null", "claimant.address.postcode")
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
        assertRequestCouldNotBeParsedErrorResponse(response, expectedField, expectedErrorMessage);
    }

    @Test
    void shouldReturnErrorGivenAnEmptyClaim() {
        //Given
        String claim = "{}";
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, "claimant", "must not be null");
    }

    private void assertClaimPersistedSuccessfully(ClaimDTO claimDTO,
                                                  EligibilityStatus eligibilityStatus,
                                                  String dwpHouseholdIdentifier,
                                                  String hmrcHouseholdIdentifier) {
        Iterable<Claim> claims = claimRepository.findAll();
        assertThat(claims).hasSize(1);
        Claim persistedClaim = claims.iterator().next();
        assertClaimantMatchesClaimantDTO(claimDTO.getClaimant(), persistedClaim.getClaimant());
        assertThat(persistedClaim.getEligibilityStatus()).isEqualTo(eligibilityStatus);
        assertThat(persistedClaim.getDwpHouseholdIdentifier()).isEqualTo(dwpHouseholdIdentifier);
        assertThat(persistedClaim.getHmrcHouseholdIdentifier()).isEqualTo(hmrcHouseholdIdentifier);
    }

    private void assertDuplicateResponse(ResponseEntity<ClaimResultDTO> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(DUPLICATE);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(response.getBody().getVoucherEntitlement()).isNull();
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

}
