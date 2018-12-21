package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.util.List;

/**
 * JPA repository for the Claimant table.
 */
public interface ClaimantRepository extends CrudRepository<Claimant, Long> {

    /**
     * Find all Claimants with the given firstName and secondName.
     *
     * @param firstName  The first name
     * @param secondName The second name
     * @return The list of claimants
     */
    List<Claimant> findByFirstNameAndSecondName(String firstName, String secondName);

}
