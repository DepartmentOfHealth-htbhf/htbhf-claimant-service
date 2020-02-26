package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimToClaimResponseDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.*;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.dhsc.htbhf.claimant.testsupport.WiremockManager;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.dhsc.htbhf.TestConstants.*;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertInternalServerErrorResponse;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertRequestCouldNotBeParsedErrorResponse;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertValidationErrorInResponse;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.anAddressDTOWithLine1;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.anAddressDTOWithPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithAddress;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithEmailAddressAndPhoneNumber;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithLastName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithNino;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithNino;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantInSameHouseholdBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResultWithPhoneAndEmail;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_SUPPLIED;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ERROR;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndDwpHouseIdentifier;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndHmrcHouseIdentifier;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
class ClaimantServiceIntegrationTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ClaimRepository claimRepository;

    @Autowired
    RepositoryMediator repositoryMediator;

    @Autowired
    WiremockManager wiremockManager;

    @Autowired
    ClaimToClaimResponseDTOConverter claimToClaimResponseDTOConverter;

    @BeforeEach
    void setup() {
        wiremockManager.startWireMock();
    }

    @AfterEach
    void cleanup() {
        repositoryMediator.deleteAllEntities();
        wiremockManager.stopWireMock();
    }

    @Test
    void shouldGetClaimById() {
        // Given
        Claim claim = aValidClaim();
        claimRepository.save(claim);

        // When
        ResponseEntity<ClaimDTO> response = restTemplate.exchange(buildRetrieveClaimRequestEntity(claim.getId()), ClaimDTO.class);

        // Then
        ClaimDTO claimResponse = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(claimResponse).isEqualToComparingOnlyGivenFields(claim,
                "id", "cardAccountId", "cardStatus", "claimStatus", "currentIdentityAndEligibilityResponse",
                "dwpHouseholdIdentifier", "hmrcHouseholdIdentifier", "eligibilityStatus",
                "initialIdentityAndEligibilityResponse");
        assertThat(claimResponse.getCardStatusTimestamp()).isEqualToIgnoringNanos(claim.getCardStatusTimestamp());
        assertThat(claimResponse.getClaimant()).isEqualToIgnoringGivenFields(claim.getClaimant(), "address");
        assertThat(claimResponse.getClaimant().getAddress()).isEqualToComparingFieldByField(claim.getClaimant().getAddress());
    }

    @Test
    void shouldGetAllClaims() {
        // Given
        Claim homerClaim = aValidClaimWithNinoAndRefernce(HOMER_NINO, HOMER_CLAIM_REFERENCE);
        claimRepository.saveAll(List.of(homerClaim));
        List<ClaimResponseDTO> claimResponseDTO = claimToClaimResponseDTOConverter.convert(List.of(homerClaim));

        // When
        ResponseEntity<List<ClaimResponseDTO>> response =
                restTemplate.exchange(buildRetrieveAllClaimEntity(), new ParameterizedTypeReference<List<ClaimResponseDTO>>() {});

        // Then
        List<ClaimResponseDTO> claimResponse = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(claimResponse).isNotNull();
        assertThat(claimResponse).hasSameSizeAs(List.of(homerClaim));
        assertThat(claimResponse.get(0)).isEqualToComparingOnlyGivenFields(claimResponseDTO.get(0),
                "id", "claimStatus", "firstName", "lastName", "dateOfBirth", "addressLine1", "postcode", "reference");
        assertThat(claimResponse.get(0).getFirstName()).isEqualTo(homerClaim.getClaimant().getFirstName());
        assertThat(claimResponse.get(0).getReference()).isEqualTo(homerClaim.getReference());
    }

    @Test
    void shouldReturnAnEmptyListWhenNoClaimsExists() {
        //when
        ResponseEntity<List<ClaimResponseDTO>> response =
                restTemplate.exchange(buildRetrieveAllClaimEntity(), new ParameterizedTypeReference<List<ClaimResponseDTO>>() {});

        //then
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenClaimWithGivenIdDoesNotExist() {
        UUID claimId = UUID.randomUUID();

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildRetrieveClaimRequestEntity(claimId), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(NOT_FOUND.value());
        assertThat(response.getBody().getMessage()).isEqualTo("Unable to find claim with id " + claimId.toString());
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getRequestId()).isNotNull();
    }

    @Test
    void shouldAcceptAndCreateANewValidClaimWithNoNullFields() throws JsonProcessingException {
        //Given
        NewClaimDTO claim = aValidClaimDTOWithNoNullFields();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);
        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ClaimResultDTO.class);
        //Then
        assertThatClaimResultHasNewClaim(response);
        assertClaimPersistedSuccessfully(claim, ELIGIBLE);
        wiremockManager.assertThatEligibilityRequestMade();
    }

    @Test
    void shouldIgnoreUnknownFieldsInValidClaimRequest() throws JsonProcessingException {
        //Given
        NewClaimDTO claim = aValidClaimDTOWithNoNullFields();
        String webUiVersionProperty = "\"" + claim.getWebUIVersion() + "\",";
        String json = objectMapper.writeValueAsString(claim).replace(webUiVersionProperty, webUiVersionProperty + " \"foo\":\"bar\",");
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(json), ClaimResultDTO.class);

        //Then
        assertThatClaimResultHasNewClaim(response);
        assertClaimPersistedSuccessfully(claim, ELIGIBLE);
        wiremockManager.assertThatEligibilityRequestMade();
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForMissingAddressFields")
    void shouldAcceptAndCreateANewValidClaimWithMissingAddressFields(String county) throws JsonProcessingException {
        //Given
        NewClaimDTO claim = aClaimDTOWithCounty(county);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ClaimResultDTO.class);

        //Then
        assertThatClaimResultHasNewClaim(response);
        assertClaimPersistedSuccessfully(claim, ELIGIBLE);
        wiremockManager.assertThatEligibilityRequestMade();
    }

    @Test
    void shouldRejectEligibleClaimantWhenNoChildrensDatesOfBirthMatch() throws JsonProcessingException {
        NewClaimDTO claim = aValidClaimDTOWithExpectedDeliveryDateAndChildrenDob(null, List.of(LocalDate.now().minusDays(1)));
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ClaimResultDTO.class);

        //Then
        assertRejectedResponse(response, ELIGIBLE);
        assertThat(response.getBody().getVerificationResult().getIsPregnantOrAtLeast1ChildMatched()).isFalse();
        wiremockManager.assertThatEligibilityRequestMade();
    }

    private static Stream<Arguments> provideArgumentsForMissingAddressFields() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of((Object) null)
        );
    }

    @Test
    void shouldFailWhenEligibilityServiceCallThrowsException() {
        //Given
        NewClaimDTO claim = aValidClaimDTO();
        wiremockManager.stubErrorEligibilityResponse();

        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ErrorResponse.class);

        //Then
        assertInternalServerErrorResponse(response);
        assertClaimPersistedWithError(claim);
        wiremockManager.assertThatEligibilityRequestMade();
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
        NewClaimDTO dto = aValidClaimDTO();
        Claimant claimant = aValidClaimantInSameHouseholdBuilder().build();
        Claim claim = aValidClaimBuilder()
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .claimant(claimant)
                .build();
        claimRepository.save(claim);

        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse
                = anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndDwpHouseIdentifier(DWP_HOUSEHOLD_IDENTIFIER);
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(dto), ClaimResultDTO.class);

        //Then
        assertRejectedResponse(response, DUPLICATE);
        wiremockManager.assertThatEligibilityRequestMade();
    }

    @Test
    void shouldReturnDuplicateStatusWhenTwoSimultaneousClaimsForSameNino() throws JsonProcessingException, InterruptedException, ExecutionException {
        //Given
        NewClaimDTO dto = aValidClaimDTO();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse
                = anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndDwpHouseIdentifier(DWP_HOUSEHOLD_IDENTIFIER);
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        //When
        CompletableFuture<ResponseEntity<ClaimResultDTO>> future1
                = CompletableFuture.supplyAsync(() -> restTemplate.exchange(buildCreateClaimRequestEntity(dto), ClaimResultDTO.class), executor);
        CompletableFuture<ResponseEntity<ClaimResultDTO>> future2
                = CompletableFuture.supplyAsync(() -> restTemplate.exchange(buildCreateClaimRequestEntity(dto), ClaimResultDTO.class), executor);

        CompletableFuture.allOf(future1, future2).get();

        List<ResponseEntity<ClaimResultDTO>> results = List.of(future1.get(), future2.get());

        //Then
        //one transaction will succeed because it commits first.
        assertThat(results.stream().anyMatch(r -> r.getStatusCode() == HttpStatus.CREATED)).isTrue();

        //one transaction will fail causing error because there is already an active claim with same nino.
        assertThat(results.stream().anyMatch(r -> r.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)).isTrue();
    }

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsForHmrcHouseholdIdentifier() throws JsonProcessingException {
        //Given
        NewClaimDTO dto = aValidClaimDTO();
        Claimant claimant = aValidClaimantInSameHouseholdBuilder().build();
        Claim claim = aValidClaimBuilder()
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .claimant(claimant)
                .build();
        claimRepository.save(claim);

        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse
                = anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndHmrcHouseIdentifier(HMRC_HOUSEHOLD_IDENTIFIER);
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(dto), ClaimResultDTO.class);

        //Then
        assertRejectedResponse(response, DUPLICATE);
    }

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsWithSameNino() {
        //Given
        NewClaimDTO dto = aClaimDTOWithClaimant(aClaimantDTOWithNino(HOMER_NINO));
        Claimant claimant = aClaimantWithNino(HOMER_NINO);
        Claim claim = aValidClaimBuilder()
                .claimant(claimant)
                .build();
        claimRepository.save(claim);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(dto), ClaimResultDTO.class);

        //Then
        assertRejectedResponse(response, DUPLICATE);
    }

    @Test
    void shouldFailWithClaimantValidationError() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithLastName(null);
        NewClaimDTO claim = aClaimDTOWithClaimant(claimant);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, "claimant.lastName", "must not be null");
    }

    @Test
    void shouldValidateClaimantWithoutPhoneNumberAndEmail() throws JsonProcessingException {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithEmailAddressAndPhoneNumber(null, null);
        NewClaimDTO claim = aClaimDTOWithClaimant(claimant);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches().toBuilder()
                .mobilePhoneMatch(NOT_SUPPLIED)
                .emailAddressMatch(NOT_SUPPLIED)
                .build();
        wiremockManager.stubEligibilityResponse(identityAndEligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ClaimResultDTO.class);
        //Then
        assertThatClaimResultHasNewClaimWithoutPhoneAndEmail(response);
        wiremockManager.assertThatEligibilityRequestMade();
    }

    @Test
    void shouldFailWithAddressValidationError() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithLine1(null);
        ClaimantDTO claimant = aClaimantDTOWithAddress(addressDTO);
        NewClaimDTO claim = aClaimDTOWithClaimant(claimant);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, "claimant.address.addressLine1", "must not be null");
    }

    @Test
    void shouldFailWithChannelIslandPostcode() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithPostcode("GY11AA");
        ClaimantDTO claimant = aClaimantDTOWithAddress(addressDTO);
        NewClaimDTO claim = aClaimDTOWithClaimant(claimant);
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ErrorResponse.class);
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
        ResponseEntity<ErrorResponse> response
                = restTemplate.exchange(buildCreateClaimRequestEntity(claimWithInvalidDate), ErrorResponse.class);
        //Then
        assertRequestCouldNotBeParsedErrorResponse(response, expectedField, expectedErrorMessage);
    }

    @Test
    void shouldReturnErrorGivenAnEmptyClaim() {
        //Given
        String claim = "{}";
        //When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(buildCreateClaimRequestEntity(claim), ErrorResponse.class);
        //Then
        assertValidationErrorInResponse(response, "claimant", "must not be null");
    }

    private void assertRejectedResponse(ResponseEntity<ClaimResultDTO> response, EligibilityStatus eligibilityStatus) {
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(eligibilityStatus);
        assertThat(response.getBody().getVoucherEntitlement()).isNull();
    }

    private void assertThatClaimResultHasNewClaimWithoutPhoneAndEmail(ResponseEntity<ClaimResultDTO> response) {
        assertThatValidClaimResult(response);
        assertThat(response.getBody().getVerificationResult()).isEqualTo(anAllMatchedVerificationResultWithPhoneAndEmail(NOT_SUPPLIED, NOT_SUPPLIED));
    }

    private void assertThatClaimResultHasNewClaim(ResponseEntity<ClaimResultDTO> response) {
        assertThatValidClaimResult(response);
        assertThat(response.getBody().getVerificationResult()).isEqualTo(anAllMatchedVerificationResult());
    }

    private void assertThatValidClaimResult(ResponseEntity<ClaimResultDTO> response) {
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(response.getBody().getVoucherEntitlement()).isEqualTo(aValidVoucherEntitlementDTO());
    }

    private void assertClaimPersistedSuccessfully(NewClaimDTO newClaimDTO,
                                                  EligibilityStatus eligibilityStatus) {
        Claim persistedClaim = assertClaimPersistedWithEligibilityStatus(newClaimDTO, eligibilityStatus);
        assertThat(persistedClaim.getDwpHouseholdIdentifier()).isEqualTo(DWP_HOUSEHOLD_IDENTIFIER);
        assertThat(persistedClaim.getHmrcHouseholdIdentifier()).isEqualTo(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    private void assertClaimPersistedWithError(NewClaimDTO newClaimDTO) {
        assertClaimPersistedWithEligibilityStatus(newClaimDTO, ERROR);
    }

    private Claim assertClaimPersistedWithEligibilityStatus(NewClaimDTO newClaimDTO, EligibilityStatus eligibilityStatus) {
        Iterable<Claim> claims = claimRepository.findAll();
        assertThat(claims).hasSize(1);
        Claim persistedClaim = claims.iterator().next();
        assertClaimantMatchesClaimantDTO(newClaimDTO.getClaimant(), persistedClaim.getClaimant());
        assertThat(persistedClaim.getEligibilityStatus()).isEqualTo(eligibilityStatus);
        return persistedClaim;
    }


    private String modifyFieldOnClaimantInJson(Object originalValue, String fieldName, String newValue) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(originalValue);
        JSONObject jsonObject = new JSONObject(json);
        jsonObject.getJSONObject("claimant").put(fieldName, newValue);
        return jsonObject.toString();
    }
}
