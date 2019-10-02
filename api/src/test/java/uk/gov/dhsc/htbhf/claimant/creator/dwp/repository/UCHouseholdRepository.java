package uk.gov.dhsc.htbhf.claimant.creator.dwp.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc.UCHousehold;

import java.util.UUID;

/**
 * JPA gov.uk.dhsc.claimant.creator.repository for Universal Credit households (members of the household are persisted via the household).
 */
public interface UCHouseholdRepository extends CrudRepository<UCHousehold, UUID> {
}
