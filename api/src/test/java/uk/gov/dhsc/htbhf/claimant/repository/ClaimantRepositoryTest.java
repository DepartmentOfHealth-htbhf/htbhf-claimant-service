package uk.gov.dhsc.htbhf.claimant.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import javax.validation.ConstraintViolationException;

import static com.google.common.collect.Iterables.size;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus.INELIGIBLE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithLastName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithTooLongFirstName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantWithEligibilityStatus;

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
        Claimant claimant = aValidClaimantWithEligibilityStatus();

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
        Throwable thrown = catchThrowable(() -> {
            claimantRepository.save(invalidClaimant);
        });
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
    void shouldReturnClaimantDoesNotExistWhenNinoDoesNotExist() {
        //Given
        Claimant claimant = aValidClaimantBuilder().build();

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForNino(claimant.getNino());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @Test
    void shouldReturnClaimantDoesNotExistWhenNinoExistsAndStatusIsNotEligible() {
        //Given
        Claimant claimant = aValidClaimantBuilder().eligibilityStatus(INELIGIBLE).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForNino(claimant.getNino());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @Test
    void shouldReturnClaimantExistsWhenNinoExistsAndStatusIsEligible() {
        //Given
        Claimant claimant = aValidClaimantBuilder().eligibilityStatus(ELIGIBLE).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForNino(claimant.getNino());

        //Then
        assertThat(claimantExists).isTrue();
    }

    @Test
    void shouldReturnClaimantDoesNotExistWhenDwpHouseholdIdentifierDoesNotExist() {
        //Given
        Claimant claimant = aValidClaimantBuilder().build();

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @Test
    void shouldReturnClaimantDoesNotExistWhenDwpHouseholdIdentifierExistsAndStatusIsNotEligible() {
        //Given
        Claimant claimant = aValidClaimantBuilder().eligibilityStatus(INELIGIBLE).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @Test
    void shouldReturnClaimantExistsWhenDwpHouseholdIdentifierExistsAndStatusIsEligible() {
        //Given
        Claimant claimant = aValidClaimantBuilder().eligibilityStatus(ELIGIBLE).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForDwpHousehold(claimant.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isTrue();
    }

    @Test
    void shouldReturnClaimantDoesNotExistWhenHmrcHouseholdIdentifierDoesNotExist() {
        //Given
        Claimant claimant = aValidClaimantBuilder().build();

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @Test
    void shouldReturnClaimantDoesNotExistWhenHmrcHouseholdIdentifierExistsAndStatusIsNotEligible() {
        //Given
        Claimant claimant = aValidClaimantBuilder().eligibilityStatus(INELIGIBLE).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isFalse();
    }

    @Test
    void shouldReturnClaimantExistsWhenHmrcHouseholdIdentifierExistsAndStatusIsEligible() {
        //Given
        Claimant claimant = aValidClaimantBuilder().eligibilityStatus(ELIGIBLE).build();
        claimantRepository.save(claimant);

        //When
        Boolean claimantExists = claimantRepository.eligibleClaimExistsForHmrcHousehold(claimant.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimantExists).isTrue();
    }
}
