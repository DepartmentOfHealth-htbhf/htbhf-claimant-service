package uk.gov.dhsc.htbhf.claimant.repository;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
@AllArgsConstructor
class ClaimLazyLoaderImpl implements ClaimLazyLoader {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public Claim getLazyLoadingClaim(UUID id) {
        return entityManager.getReference(Claim.class, id);
    }
}
