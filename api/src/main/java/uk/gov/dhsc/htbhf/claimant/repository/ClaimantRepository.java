package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.util.UUID;

/**
 * JPA repository for the Claimant table.
 */
public interface ClaimantRepository extends CrudRepository<Claimant, UUID> {

}
