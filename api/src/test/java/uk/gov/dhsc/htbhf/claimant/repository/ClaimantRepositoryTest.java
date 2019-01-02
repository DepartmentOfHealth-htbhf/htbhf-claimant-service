package uk.gov.dhsc.htbhf.claimant.repository;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
class ClaimantRepositoryTest {

    @Autowired
    private ClaimantRepository claimantRepository;

    @Test
    void saveAndRetrieveClaimant() {
        //Given
        Claimant claimant = aValidClaimant();

        //When
        Claimant savedClaimant = claimantRepository.save(claimant);

        //Then
        assertThat(savedClaimant.getId()).isNotNull();
        assertClaimantsMatchIgnoringId(savedClaimant, claimant);
    }

    private void assertClaimantsMatchIgnoringId(Claimant actualClaimant, Claimant expectedClaimant) {
        assertThat(expectedClaimant).isNotNull();
        assertThat(actualClaimant).isNotNull();
        assertThat(actualClaimant.getFirstName()).isEqualTo(expectedClaimant.getFirstName());
        assertThat(actualClaimant.getSecondName()).isEqualTo(expectedClaimant.getSecondName());
    }
}
