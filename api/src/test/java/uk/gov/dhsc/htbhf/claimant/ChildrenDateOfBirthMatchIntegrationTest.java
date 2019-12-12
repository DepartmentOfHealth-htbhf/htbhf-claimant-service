package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.dhsc.htbhf.claimant.testsupport.WiremockManager;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.dhsc.htbhf.TestConstants.LISA_DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_AND_LISA_DOBS;
import static uk.gov.dhsc.htbhf.TestConstants.SINGLE_SIX_MONTH_OLD;
import static uk.gov.dhsc.htbhf.TestConstants.SINGLE_THREE_YEAR_OLD;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.buildClaimRequestEntity;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.NEW;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.REJECTED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithExpectedDeliveryDateAndChildrenDob;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class ChildrenDateOfBirthMatchIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WiremockManager wiremockManager;

    @Autowired
    private RepositoryMediator repositoryMediator;

    @BeforeEach
    void init() {
        wiremockManager.startWireMock();
    }

    @AfterEach
    void cleanup() {
        wiremockManager.stopWireMock();
        repositoryMediator.deleteAllEntities();
    }

    @ParameterizedTest
    @MethodSource("datesOfBirthForValidClaims")
    void shouldCreateNewClaimWhenPregnantOrChildDatesOfBirthMatch(LocalDate expectedDeliveryDate,
                              List<LocalDate> declaredChildrenDob,
                              List<LocalDate> childrenDobFromBenefitAgency) throws JsonProcessingException {
        ClaimDTO claim = aValidClaimDTOWithExpectedDeliveryDateAndChildrenDob(expectedDeliveryDate, declaredChildrenDob);
        wiremockManager.stubSuccessfulEligibilityResponse(childrenDobFromBenefitAgency);

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claim), ClaimResultDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(NEW);
        assertThat(response.getBody().getVerificationResult().getIsPregnantOrAtLeast1ChildMatched()).isTrue();
    }

    // Provides arguments for successfully creating a new claim. Arguments are: expected delivery date, declared children dates of birth
    // and children dates of birth returned from benefit agency.
    private static Stream<Arguments> datesOfBirthForValidClaims() {
        return Stream.of(
                Arguments.of(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, SINGLE_THREE_YEAR_OLD, SINGLE_SIX_MONTH_OLD), // pregnant and children dob not match
                Arguments.of(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, MAGGIE_AND_LISA_DOBS, MAGGIE_AND_LISA_DOBS), // pregnant and children dob match
                Arguments.of(null, MAGGIE_AND_LISA_DOBS, MAGGIE_AND_LISA_DOBS), // not pregnant and all children match
                Arguments.of(null, MAGGIE_AND_LISA_DOBS, List.of(LISA_DATE_OF_BIRTH)) // not pregnant and some children match
        );
    }

    @Test
    @Disabled("TODO HTBHF-2727 Enable test once checking children's dob")
    void shouldRejectClaimWhenNotPregnantAndNoChildrenDobMatch() throws JsonProcessingException {
        ClaimDTO claim = aValidClaimDTOWithExpectedDeliveryDateAndChildrenDob(null, SINGLE_THREE_YEAR_OLD);
        wiremockManager.stubSuccessfulEligibilityResponse(SINGLE_SIX_MONTH_OLD);

        ResponseEntity<ClaimResultDTO> response = restTemplate.exchange(buildClaimRequestEntity(claim), ClaimResultDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody().getClaimStatus()).isEqualTo(REJECTED);
        assertThat(response.getBody().getVerificationResult().getIsPregnantOrAtLeast1ChildMatched()).isFalse();
    }
}
