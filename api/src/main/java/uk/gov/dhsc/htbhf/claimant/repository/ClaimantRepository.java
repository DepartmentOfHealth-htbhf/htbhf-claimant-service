package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.util.UUID;

/**
 * JPA repository for the Claimant table.
 */
public interface ClaimantRepository extends CrudRepository<Claimant, UUID> {

    @Query("SELECT COUNT(claimant) FROM Claimant claimant WHERE claimant.nino = :nino AND claimant.eligibilityStatus = 'ELIGIBLE'")
    Long getNumberOfMatchingEligibleClaimants(@Param("nino") String nino);

    default Boolean eligibleClaimExists(String nino) {
        return getNumberOfMatchingEligibleClaimants(nino) != 0;
    }
}
