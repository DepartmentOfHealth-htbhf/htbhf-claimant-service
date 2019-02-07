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
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithLastName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithTooLongFirstName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;

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
}
