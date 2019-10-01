package uk.gov.dhsc.htbhf.claimant.creator.repository.claimant;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.creator.entities.claimant.Claim;

import java.util.UUID;

/**
 * JPA gov.uk.dhsc.claimant.creator.repository for the Claim table.
 * For all methods regarding live claims, a claim is live if it's claim status is one of 'NEW', 'ACTIVE', 'PENDING' or 'PENDING_EXPIRY'.
 * All updates to a {@link Claim} and it's child gov.uk.dhsc.claimant.creator.entities are audited using Javers.
 * @see <a href="https://javers.org/">https://javers.org/</a> for more information.
 */
public interface ClaimRepository extends CrudRepository<Claim, UUID> {
}
