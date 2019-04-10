package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.util.UUID;

/**
 * JPA repository for the Claimant table.
 * For all methods regarding live claims, a claim is live if it's claim status is one of 'NEW', 'ACTIVE', 'PENDING' or 'PENDING_EXPIRY'.
 */
public interface ClaimantRepository extends CrudRepository<Claimant, UUID> {

    @Query("SELECT COUNT(claimant) "
            + "FROM Claimant claimant "
            + "WHERE claimant.dwpHouseholdIdentifier = :dwpHouseholdIdentifier "
            + "AND claimant.claimStatus in ('NEW', 'ACTIVE', 'PENDING', 'PENDING_EXPIRY')")
    Long countLiveClaimantsWithDwpHouseholdIdentifier(@Param("dwpHouseholdIdentifier") String dwpHouseholdIdentifier);

    @Query("SELECT COUNT(claimant) "
            + "FROM Claimant claimant "
            + "WHERE claimant.hmrcHouseholdIdentifier = :hmrcHouseholdIdentifier "
            + "AND claimant.claimStatus in ('NEW', 'ACTIVE', 'PENDING', 'PENDING_EXPIRY')")
    Long countLiveClaimantsWithHmrcHouseholdIdentifier(@Param("hmrcHouseholdIdentifier") String hmrcHouseholdIdentifier);

    @Query("SELECT COUNT(claimant) "
            + "FROM Claimant claimant "
            + "WHERE claimant.nino = :nino "
            + "AND claimant.claimStatus in ('NEW', 'ACTIVE', 'PENDING', 'PENDING_EXPIRY')")
    Long countLiveClaimantsWithNino(@Param("nino") String nino);

    default boolean liveClaimExistsForDwpHousehold(String dwpHouseholdIdentifier) {
        return countLiveClaimantsWithDwpHouseholdIdentifier(dwpHouseholdIdentifier) != 0;
    }

    default boolean liveClaimExistsForHmrcHousehold(String hmrcHouseholdIdentifier) {
        return countLiveClaimantsWithHmrcHouseholdIdentifier(hmrcHouseholdIdentifier) != 0;
    }

    default boolean liveClaimExistsForNino(String nino) {
        return countLiveClaimantsWithNino(nino) != 0;
    }
}
