package uk.gov.dhsc.htbhf.claimant.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import javax.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithTooLongFirstName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;

@SpringBootTest
class ClaimantRepositoryTest {

    @Autowired
    private ClaimantRepository claimantRepository;

    @Autowired
    private EntityManager entityManager;

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
        Throwable thrown = catchThrowableOfType(() -> {
            claimantRepository.save(invalidClaimant);
            entityManager.flush();
        }, ConstraintViolationException.class);
        //Then
        assertThat(thrown).isNotNull().hasMessageContaining("size must be between 0 and 500");
    }
}
