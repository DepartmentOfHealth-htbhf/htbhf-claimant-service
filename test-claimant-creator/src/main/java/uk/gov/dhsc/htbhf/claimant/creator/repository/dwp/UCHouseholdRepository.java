package uk.gov.dhsc.htbhf.claimant.creator.repository.dwp;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.creator.entities.dwp.uc.UCHousehold;

import java.util.UUID;

/**
 * JPA gov.uk.dhsc.claimant.creator.repository for Universal Credit households (members of the household are persisted via the household).
 */
public interface UCHouseholdRepository extends CrudRepository<UCHousehold, UUID> {
}
