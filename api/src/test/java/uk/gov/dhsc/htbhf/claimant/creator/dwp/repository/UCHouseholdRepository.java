package uk.gov.dhsc.htbhf.claimant.creator.dwp.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc.UCHousehold;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * JPA gov.uk.dhsc.claimant.creator.repository for Universal Credit households (members of the household are persisted via the household).
 */
public interface UCHouseholdRepository extends CrudRepository<UCHousehold, UUID> {

    /**
     * Return all households containing an adult with the given nino. These are ordered by fileImportNumber in descending order.
     * @param nino The nino to check against
     * @return A stream containing all households found
     */
    @Query("SELECT household FROM UCHousehold household INNER JOIN FETCH household.adults adult "
            + "WHERE adult.nino = :nino ORDER BY household.fileImportNumber DESC")
    Stream<UCHousehold> findAllHouseholdsByAdultWithNino(@Param("nino") String nino);

}
