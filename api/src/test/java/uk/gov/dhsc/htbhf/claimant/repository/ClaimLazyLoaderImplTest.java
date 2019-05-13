package uk.gov.dhsc.htbhf.claimant.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

import java.util.UUID;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
class ClaimLazyLoaderImplTest {

    @Mock
    EntityManager entityManager;

    @InjectMocks
    ClaimLazyLoaderImpl testObj;


    @Test
    void shouldGetReferenceFromEntityManager() {
        Claim claim = Claim.builder().build();
        UUID claimId = claim.getId();
        given(entityManager.getReference(any(), any())).willReturn(claim);

        Claim result = testObj.getLazyLoadingClaim(claimId);

        assertThat(result).isEqualTo(claim);
        verify(entityManager).getReference(Claim.class, claimId);
    }
}