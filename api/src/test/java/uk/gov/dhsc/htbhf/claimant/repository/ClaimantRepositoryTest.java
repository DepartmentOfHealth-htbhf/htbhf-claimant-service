package uk.gov.dhsc.htbhf.claimant.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;

import javax.validation.ConstraintViolationException;

import static com.google.common.collect.Iterables.size;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithLastName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithTooLongFirstName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;

@SpringBootTest
class ClaimantRepositoryTest {

    @Autowired
    private ClaimantRepository claimantRepository;

    @AfterEach
    void afterEach() {
        claimantRepository.deleteAll();
    }

    @Test
    void saveAndRetrieveClaimant() {
        //Given
        Claimant claimant = aValidClaimant();

        //When
        Claimant savedClaimant = claimantRepository.save(claimant);

        //Then
        assertThat(savedClaimant.getId()).isNotNull();
        assertThat(savedClaimant).isEqualTo(claimant);
    }

    @Test
    void anInvalidClaimantIsNotSaved() {
        //Given
        Claimant invalidClaimant = aClaimantWithTooLongFirstName();
        //When
        Throwable thrown = catchThrowable(() -> claimantRepository.save(invalidClaimant));
        //Then
        assertThat(thrown).hasRootCauseInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldHandleSqlInjectionAttempts() {
        //Given
        var sqlInjectionName = "Robert'); DROP TABLE CLAIMANT; --)";
        Claimant sqlInjectionClaimant = aClaimantWithLastName(sqlInjectionName);

        //When
        claimantRepository.save(sqlInjectionClaimant);

        // if the sql injection was successful then findAll will fail
        //Then
        assertThat(size(claimantRepository.findAll())).isEqualTo(1);
        assertThat(claimantRepository.findAll().iterator().next().getLastName()).isEqualTo(sqlInjectionName);
    }

    @Test
    void shouldReturnLiveClaimDoesNotExistWhenNinoDoesNotExist() {
        //Given
        Claimant claimant = aValidClaimantBuilder().build();

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForNino(claimant.getNino());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does exist when claimant exists and claim status is {0}")
    @ValueSource(strings = {
            "NEW",
            "ACTIVE",
            "PENDING",
            "PENDING_EXPIRY"
    })
    void shouldReturnLiveClaimDoesExistWhenNinoExists(ClaimStatus claimStatus) {
        //Given
        Claimant claimant = aValidClaimantBuilder().claimStatus(claimStatus).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForNino(claimant.getNino());

        //Then
        assertThat(claimantExists).isTrue();
    }

    @ParameterizedTest(name = "Should return that a live claim does not exist when claimant exists and claim status is {0}")
    @ValueSource(strings = {
            "REJECTED",
            "ERROR",
            "EXPIRED"
    })
    void shouldReturnLiveClaimDoesNotExistWhenNinoExists(ClaimStatus claimStatus) {
        //Given
        Claimant claimant = aValidClaimantBuilder().claimStatus(claimStatus).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForNino(claimant.getNino());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @Test
    void shouldReturnLiveClaimDoesNotExistWhenDwpHouseholdIdentifierDoesNotExist() {
        //Given
        Claimant claimant = aValidClaimantBuilder().build();

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does not exist when dwp household exists and the claim status is {0}")
    @ValueSource(strings = {
            "ERROR",
            "REJECTED",
            "EXPIRED"
    })
    void shouldReturnLiveClaimDoesNotExistWhenDwpHouseholdIdentifierExists(ClaimStatus claimStatus) {
        //Given
        Claimant claimant = aValidClaimantBuilder().claimStatus(claimStatus).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does exist when dwp household exists and the claim status is {0}")
    @ValueSource(strings = {
            "NEW",
            "PENDING",
            "ACTIVE",
            "PENDING_EXPIRY"
    })
    void shouldReturnLiveClaimDoesExistWhenDwpHouseholdIdentifierExists(ClaimStatus claimStatus) {
        //Given
        Claimant claimant = aValidClaimantBuilder().claimStatus(claimStatus).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isTrue();
    }

    @Test
    void shouldReturnClaimantDoesNotExistWhenHmrcHouseholdIdentifierDoesNotExist() {
        //Given
        Claimant claimant = aValidClaimantBuilder().build();

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does not exist when hmrc household exists and the claim status is {0}")
    @ValueSource(strings = {
            "ERROR",
            "REJECTED",
            "EXPIRED"
    })
    void shouldReturnLiveClaimDoesNotExistWhenHmrcHouseholdIdentifierExists(ClaimStatus claimStatus) {
        //Given
        Claimant claimant = aValidClaimantBuilder().claimStatus(claimStatus).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does exist when hmrc household exists and the claim status is {0}")
    @ValueSource(strings = {
            "NEW",
            "PENDING",
            "ACTIVE",
            "PENDING_EXPIRY"
    })
    void shouldReturnLiveClaimDoesExistWhenHmrcHouseholdIdentifierExists(ClaimStatus claimStatus) {
        //Given
        Claimant claimant = aValidClaimantBuilder().claimStatus(claimStatus).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.liveClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isTrue();
    }
}
