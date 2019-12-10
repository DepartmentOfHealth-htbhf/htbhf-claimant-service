package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimDTOV3;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.dhsc.htbhf.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_NINO_V2;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertClaimantMatchesClaimantDTO;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.buildClaimRequestEntityForUri;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aClaimDTOWithClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aValidClaimDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOV3TestDataFactory.aClaimantDTOWithNino;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithNino;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantInSameHouseholdBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndDwpHouseIdentifier;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndHmrcHouseIdentifier;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@AutoConfigureWireMock(port = 8100)
class ClaimantServiceV3IntegrationTests {

    private static final URI CLAIMANT_ENDPOINT_URI_V3 = URI.create("/v3/claims");
    private static final String ELIGIBILITY_SERVICE_URL = "/v2/eligibility";

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
        ClaimDTOV3 claim = aValidClaimDTOWithNoNullFields();
        //Given
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();
        stubEligibilityServiceWithSuccessfulResponse(identityAndEligibilityResponse);
        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntityForUri(claim, CLAIMANT_ENDPOINT_URI_V3), ClaimResultDTO.class);
        //Then
        assertThatClaimResultHasNewClaim(response);
        assertClaimPersistedSuccessfully(claim, ELIGIBLE);
        verifyPostToEligibilityService();
    }

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsForDwpHouseholdIdentifier() throws JsonProcessingException {
        //Given
        ClaimDTOV3 dto = aValidClaimDTO();
        Claimant claimant = aValidClaimantInSameHouseholdBuilder().build();
        Claim claim = aValidClaimBuilder()
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .claimant(claimant)
                .build();
        claimRepository.save(claim);

        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse
                = anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndDwpHouseIdentifier(DWP_HOUSEHOLD_IDENTIFIER);
        stubEligibilityServiceWithSuccessfulResponse(identityAndEligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntityForUri(dto, CLAIMANT_ENDPOINT_URI_V3), ClaimResultDTO.class);

        //Then
        assertDuplicateResponse(response);
        verifyPostToEligibilityService();
    }

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsForHmrcHouseholdIdentifier() throws JsonProcessingException {
        //Given
        ClaimDTOV3 dto = aValidClaimDTO();
        Claimant claimant = aValidClaimantInSameHouseholdBuilder().build();
        Claim claim = aValidClaimBuilder()
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .claimant(claimant)
                .build();
        claimRepository.save(claim);

        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse
                = anIdMatchedEligibilityConfirmedUCResponseWithAllMatchesAndHmrcHouseIdentifier(HMRC_HOUSEHOLD_IDENTIFIER);
        stubEligibilityServiceWithSuccessfulResponse(identityAndEligibilityResponse);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntityForUri(dto, CLAIMANT_ENDPOINT_URI_V3), ClaimResultDTO.class);

        //Then
        assertDuplicateResponse(response);
    }

    @Test
    void shouldReturnDuplicateStatusWhenEligibleClaimAlreadyExistsWithSameNino() {
        //Given
        ClaimDTOV3 dto = aClaimDTOWithClaimant(aClaimantDTOWithNino(HOMER_NINO_V2));
        Claimant claimant = aClaimantWithNino(HOMER_NINO_V2);
        Claim claim = aValidClaimBuilder()
                .claimant(claimant)
                .build();
        claimRepository.save(claim);

        //When
        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntityForUri(dto, CLAIMANT_ENDPOINT_URI_V3), ClaimResultDTO.class);

        //Then
        assertDuplicateResponse(response);
    }

    private void assertDuplicateResponse(ResponseEntity<ClaimResultDTO> response) {
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(DUPLICATE);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(response.getBody().getVoucherEntitlement()).isNull();
    }

    private void assertThatClaimResultHasNewClaim(ResponseEntity<ClaimResultDTO> response) {
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(response.getBody().getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(response.getBody().getVoucherEntitlement()).isEqualTo(aValidVoucherEntitlementDTO());
        assertThat(response.getBody().getVerificationResult()).isEqualTo(anAllMatchedVerificationResult());
    }

    private void assertClaimPersistedSuccessfully(ClaimDTOV3 claimDTO,
                                                  EligibilityStatus eligibilityStatus) {
        Iterable<Claim> claims = claimRepository.findAll();
        assertThat(claims).hasSize(1);
        Claim persistedClaim = claims.iterator().next();
        assertClaimantMatchesClaimantDTO(claimDTO.getClaimant(), persistedClaim.getClaimant());
        assertThat(persistedClaim.getEligibilityStatus()).isEqualTo(eligibilityStatus);
        assertThat(persistedClaim.getDwpHouseholdIdentifier()).isEqualTo(DWP_HOUSEHOLD_IDENTIFIER);
        assertThat(persistedClaim.getHmrcHouseholdIdentifier()).isEqualTo(HMRC_HOUSEHOLD_IDENTIFIER);
    }

    private void stubEligibilityServiceWithSuccessfulResponse(CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse)
            throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(identityAndEligibilityResponse);
        stubFor(post(urlEqualTo(ELIGIBILITY_SERVICE_URL)).willReturn(okJson(json)));
    }

    private void verifyPostToEligibilityService() {
        verify(exactly(1), postRequestedFor(urlEqualTo(ELIGIBILITY_SERVICE_URL)));
    }
}
