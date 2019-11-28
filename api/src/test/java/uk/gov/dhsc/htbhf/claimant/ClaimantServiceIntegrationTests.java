package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.v2.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.v2.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.v2.ClaimantDTO;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertInternalServerErrorResponse;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertRequestCouldNotBeParsedErrorResponse;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertValidationErrorInResponse;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertClaimantMatchesClaimantDTO;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.buildClaimRequestEntity;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.EXPECTED_DELIVERY_DATE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTOBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.anAddressDTOWithLine1;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.anAddressDTOWithPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aClaimDTOWithClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithAddress;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithPhoneNumber;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantInSameHouseholdBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithDwpHouseholdIdentifier;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithHmrcHouseholdIdentifier;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ERROR;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@AutoConfigureWireMock(port = 8100)
class ClaimantServiceIntegrationTests {

    private static final String ELIGIBILITY_SERVICE_URL = "/v1/eligibility";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ClaimRepository claimRepository;

    @Autowired
    RepositoryMediator repositoryMediator;

    @AfterEach
    void cleanup() {
        repositoryMediator.deleteAllEntities();
        WireMock.reset();
    }

    @Test
    void shouldAcceptAndCreateANewValidClaimWithNoNullFields() throws JsonProcessingException {
        shouldAcceptAndCreateValidClaim(aValidClaimDTOWithNoNullFields());
    }

    @Test
    void shouldIgnoreUnknownFieldsInValidClaimRequest() throws JsonProcessingException {
        ClaimDTO claim = aValidClaimDTOWithNoNullFields();
        String webUiVersionProperty = "\"" + claim.getWebUIVersion() + "\",";
        String json = objectMapper.writeValueAsString(claim)
                .replace(webUiVersionProperty, webUiVersionProperty + " \"foo\":\"bar\",");
        shouldAcceptAndCreateValidClaimFromJson(json);
    }

    @Test
    void shouldAcceptAndCreateANewValidClaimWithNullFields() throws JsonProcessingException {
        shouldAcceptAndCreateValidClaim(aValidClaimDTO());
    }

    @Test
    void shouldAcceptAndCreateANewValidClaimWithSomeEmptyAddressFields() throws JsonProcessingException {
        AddressDTO addressWithEmptyLine2AndCounty = aValidAddressDTOBuilder()
                .addressLine2("")
                .county("")
                .build();
        shouldAcceptAndCreateValidClaim(aClaimDTOWithClaimant(aClaimantDTOWithAddress(addressWithEmptyLine2AndCounty)));
    }

    @Test
    void shouldAcceptAndUpdateAnExistingEligibleClaim() throws JsonProcessingException {
        //Given
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        stubEligibilityServiceWithSuccessfulResponse(eligibilityResponse);
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(7);
        ClaimDTO claim = aValidClaimDTOWithExpectedDeliveryDate(expectedDeliveryDate);
        saveActiveClaimWithNinoAndNoExpectedDeliveryDate(claim.getClaimant().getNino());

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claim), ClaimResultDTO.class);

        //Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ACTIVE);
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(response.getBody().getVoucherEntitlement()).isEqualTo(aValidVoucherEntitlementDTO());
        assertThat(response.getBody().getClaimUpdated()).isTrue();
        assertThat(response.getBody().getUpdatedFields()).contains(EXPECTED_DELIVERY_DATE.getFieldName());
        assertThat(response.getBody().getVerificationResult()).isEqualTo(anAllMatchedVerificationResult());
        assertClaimUpdatedSuccessfully(expectedDeliveryDate);
        verifyPostToEligibilityService();
    }

    @Test
    void shouldFailWhenEligibilityServiceCallThrowsException() {
        //Given
        ClaimDTO claim = aValidClaimDTO();
        stubEligibilityServiceWithInternalServiceError();
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildClaimRequestEntity(claim), ErrorResponse.class);
        //Then
        assertInternalServerErrorResponse(response);
        assertClaimPersistedSuccessfully(claim, ERROR, null, null);
    }

    @Test
    void shouldReturn404ErrorForNonExistentPath() {
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/missing-resource", ErrorResponse.class);
        //Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsForDwpHouseholdIdentifier() throws JsonProcessingException {
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
        stubEligibilityServiceWithSuccessfulResponse(eligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(dto), ClaimResultDTO.class);

        //Then
        assertDuplicateResponse(response);
        verifyPostToEligibilityService();
    }

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsForHmrcHouseholdIdentifier() throws JsonProcessingException {
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
        stubEligibilityServiceWithSuccessfulResponse(eligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(dto), ClaimResultDTO.class);

        //Then
        assertDuplicateResponse(response);
    }

    @Test
    void shouldFailWithClaimantValidationError() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithPhoneNumber(null);
        ClaimDTO claim = aClaimDTOWithClaimant(claimant);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildClaimRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, "claimant.phoneNumber", "must not be null");
    }

    @Test
    void shouldFailWithAddressValidationError() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithLine1(null);
        ClaimantDTO claimant = aClaimantDTOWithAddress(addressDTO);
        ClaimDTO claim = aClaimDTOWithClaimant(claimant);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildClaimRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, "claimant.address.addressLine1", "must not be null");
    }

    @Test
    void shouldFailWithChannelIslandPostcode() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithPostcode("GY11AA");
        ClaimantDTO claimant = aClaimantDTOWithAddress(addressDTO);
        ClaimDTO claim = aClaimDTOWithClaimant(claimant);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildClaimRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, "claimant.address.postcode", "postcodes in the Channel Islands or Isle of Man are not acceptable");
    }

    @ParameterizedTest(name = "Field {1} with invalid value {0} on an error response")
    @CsvSource({
            "29-11-1909, dateOfBirth, '29-11-1909' could not be parsed as a LocalDate, claimant.dateOfBirth",
            "1999/12/31, dateOfBirth, '1999/12/31' could not be parsed as a LocalDate, claimant.dateOfBirth",
            "Foo, dateOfBirth, 'Foo' could not be parsed as a LocalDate, claimant.dateOfBirth",
            "29-11-1909, expectedDeliveryDate, '29-11-1909' could not be parsed as a LocalDate, claimant.expectedDeliveryDate",
            "1999/12/31, expectedDeliveryDate, '1999/12/31' could not be parsed as a LocalDate, claimant.expectedDeliveryDate",
            "Foo, expectedDeliveryDate, 'Foo' could not be parsed as a LocalDate, claimant.expectedDeliveryDate"
    })
    void shouldFailWithInvalidDateFormatError(String dateString, String fieldName, String expectedErrorMessage, String expectedField)
            throws JsonProcessingException {
        //Given
        String claimWithInvalidDate = modifyFieldOnClaimantInJson(aValidClaimDTO(), fieldName, dateString);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildClaimRequestEntity(claimWithInvalidDate), ErrorResponse.class);
        //Then
        assertRequestCouldNotBeParsedErrorResponse(response, expectedField, expectedErrorMessage);
    }

    @Test
    void shouldReturnErrorGivenAnEmptyClaim() {
        //Given
        String claim = "{}";
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildClaimRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, "claimant", "must not be null");
    }

    private void shouldAcceptAndCreateValidClaim(ClaimDTO claim) throws JsonProcessingException {
        //Given
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        stubEligibilityServiceWithSuccessfulResponse(eligibilityResponse);
        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claim), ClaimResultDTO.class);
        //Then
        assertThatClaimResultHasNewClaim(response);
        assertClaimPersistedSuccessfully(claim, ELIGIBLE, "dwpHousehold1", "hmrcHousehold1");
        verifyPostToEligibilityService();
    }

    private void shouldAcceptAndCreateValidClaimFromJson(String claimJson) throws JsonProcessingException {
        //Given
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        stubEligibilityServiceWithSuccessfulResponse(eligibilityResponse);
        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claimJson), ClaimResultDTO.class);
        //Then
        assertThatClaimResultHasNewClaim(response);
        verifyPostToEligibilityService();
    }

    private void assertThatClaimResultHasNewClaim(ResponseEntity<ClaimResultDTO> response) {
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(response.getBody().getVoucherEntitlement()).isEqualTo(aValidVoucherEntitlementDTO());
        assertThat(response.getBody().getVerificationResult()).isEqualTo(anAllMatchedVerificationResult());
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

    private void assertClaimUpdatedSuccessfully(LocalDate expectedDeliveryDate) {
        Iterable<Claim> claims = claimRepository.findAll();
        assertThat(claims).hasSize(1);
        Claim claim = claims.iterator().next();
        assertThat(claim.getClaimant().getExpectedDeliveryDate()).isEqualTo(expectedDeliveryDate);
    }

    private void assertDuplicateResponse(ResponseEntity<ClaimResultDTO> response) {
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(DUPLICATE);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(response.getBody().getVoucherEntitlement()).isNull();
        assertThat(response.getBody().getVerificationResult()).isEqualTo(anAllMatchedVerificationResult());
    }

    private String modifyFieldOnClaimantInJson(Object originalValue, String fieldName, String newValue) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(originalValue);
        JSONObject jsonObject = new JSONObject(json);
        jsonObject.getJSONObject("claimant").put(fieldName, newValue);
        return jsonObject.toString();
    }

    private void saveActiveClaimWithNinoAndNoExpectedDeliveryDate(String nino) {
        Claimant claimant = aValidClaimantBuilder()
                .nino(nino)
                .expectedDeliveryDate(null)
                .build();
        Claim claim = aValidClaimBuilder()
                .claimStatus(ACTIVE)
                .claimant(claimant)
                .build();
        claimRepository.save(claim);
    }

    private void stubEligibilityServiceWithSuccessfulResponse(EligibilityResponse eligibilityResponse) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(eligibilityResponse);
        stubFor(post(urlEqualTo(ELIGIBILITY_SERVICE_URL)).willReturn(okJson(json)));
    }

    private void stubEligibilityServiceWithInternalServiceError() {
        stubFor(post(urlEqualTo(ELIGIBILITY_SERVICE_URL)).willReturn(responseDefinition().withStatus(INTERNAL_SERVER_ERROR.value())));
    }

    private void verifyPostToEligibilityService() {
        verify(exactly(1), postRequestedFor(urlEqualTo(ELIGIBILITY_SERVICE_URL)));
    }
}
