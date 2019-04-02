package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for the Claimant table.
 */
public interface ClaimantRepository extends CrudRepository<Claimant, UUID> {

    @Query("SELECT COUNT(claimant) "
            + "FROM Claimant claimant "
            + "WHERE claimant.nino = :nino "
            + "AND claimant.eligibilityStatus = 'ELIGIBLE'")
    Long countEligibleClaimantsWithNino(@Param("nino") String nino);

    @Query("SELECT COUNT(claimant) "
            + "FROM Claimant claimant "
            + "WHERE claimant.dwpHouseholdIdentifier = :dwpHouseholdIdentifier "
            + "AND claimant.eligibilityStatus = 'ELIGIBLE'")
    Long countEligibleClaimantsWithDwpHouseholdIdentifier(@Param("dwpHouseholdIdentifier") String dwpHouseholdIdentifier);

    @Query("SELECT COUNT(claimant) "
            + "FROM Claimant claimant "
            + "WHERE claimant.hmrcHouseholdIdentifier = :hmrcHouseholdIdentifier "
            + "AND claimant.eligibilityStatus = 'ELIGIBLE'")
    Long countEligibleClaimantsWithHmrcHouseholdIdentifier(@Param("hmrcHouseholdIdentifier") String hmrcHouseholdIdentifier);

    default boolean eligibleClaimExistsForNino(String nino) {
        return countEligibleClaimantsWithNino(nino) != 0;
    }

    default boolean eligibleClaimExistsForHousehold(Optional<String> dwpHouseholdIdentifier, Optional<String> hmrcHouseholdIdentifier) {
        if (dwpHouseholdIdentifier.isPresent()) {
            return countEligibleClaimantsWithDwpHouseholdIdentifier(dwpHouseholdIdentifier.get()) != 0;
        }

        if (hmrcHouseholdIdentifier.isPresent()) {
            return countEligibleClaimantsWithHmrcHouseholdIdentifier(hmrcHouseholdIdentifier.get()) != 0;
        }

        return false;
    }
}
