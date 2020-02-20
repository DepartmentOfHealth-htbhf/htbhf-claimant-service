package uk.gov.dhsc.htbhf.claimant.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.dhsc.htbhf.claimant.entity.CardStatus;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.exception.MultipleClaimsWithSameNinoException;
import uk.gov.dhsc.htbhf.database.exception.EntityNotFoundException;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.PENDING_CANCELLATION;

/**
 * JPA repository for the Claim table.
 * For all methods regarding live claims, a claim is live if it's claim status is one of 'NEW', 'ACTIVE', 'PENDING' or 'PENDING_EXPIRY'.
 * All updates to a {@link Claim} and it's child entities are audited using Javers.
 *
 * @see <a href="https://javers.org/">https://javers.org/</a> for more information.
 */
@JaversSpringDataAuditable
public interface ClaimRepository extends CrudRepository<Claim, UUID>, ClaimLazyLoader {

    @Query("SELECT claim.id "
            + "FROM Claim claim "
            + "WHERE claim.claimant.nino = :nino "
            + "AND claim.claimStatus in ('NEW', 'ACTIVE', 'PENDING', 'PENDING_EXPIRY')"
            + "ORDER BY claim.claimStatusTimestamp DESC")
    List<UUID> findLiveClaimsWithNino(@Param("nino") String nino);

    @Query("SELECT COUNT(claim) "
            + "FROM Claim claim "
            + "WHERE claim.dwpHouseholdIdentifier = :dwpHouseholdIdentifier "
            + "AND claim.claimStatus in ('NEW', 'ACTIVE', 'PENDING', 'PENDING_EXPIRY')")
    Long countLiveClaimsWithDwpHouseholdIdentifier(@Param("dwpHouseholdIdentifier") String dwpHouseholdIdentifier);

    @Query("SELECT COUNT(claim) "
            + "FROM Claim claim "
            + "WHERE claim.hmrcHouseholdIdentifier = :hmrcHouseholdIdentifier "
            + "AND claim.claimStatus in ('NEW', 'ACTIVE', 'PENDING', 'PENDING_EXPIRY')")
    Long countLiveClaimsWithHmrcHouseholdIdentifier(@Param("hmrcHouseholdIdentifier") String hmrcHouseholdIdentifier);

    @Query("SELECT claim.id FROM Claim claim where claim.claimStatus = 'NEW'")
    List<UUID> getNewClaimIds();

    /**
     * Gets the claims that have a card_status of PENDING_CANCELLATION and a card_status_timestamp older than or equal to than the given period.
     *
     * @param period length of time the card status has been PENDING_CANCELLATION.
     * @return list of claims
     */
    default List<Claim> getClaimsWithCardStatusPendingCancellationOlderThan(Period period) {
        LocalDateTime timestamp = LocalDateTime.now().minus(period);
        return getClaimsWithCardStatusBeforeGivenTimestamp(PENDING_CANCELLATION, timestamp);
    }

    @Query("SELECT claim FROM Claim claim where claim.cardStatus = :cardStatus and claim.cardStatusTimestamp <= :date")
    List<Claim> getClaimsWithCardStatusBeforeGivenTimestamp(@Param("cardStatus") CardStatus cardStatus, @Param("date") LocalDateTime localDateTime);

    default boolean liveClaimExistsForDwpHousehold(String dwpHouseholdIdentifier) {
        return countLiveClaimsWithDwpHouseholdIdentifier(dwpHouseholdIdentifier) != 0;
    }

    default boolean liveClaimExistsForHmrcHousehold(String hmrcHouseholdIdentifier) {
        return countLiveClaimsWithHmrcHouseholdIdentifier(hmrcHouseholdIdentifier) != 0;
    }

    default Claim findClaim(UUID claimId) {
        Optional<Claim> optionalClaim = findById(claimId);
        return optionalClaim.orElseThrow(() -> new EntityNotFoundException("Unable to find claim with id " + claimId));
    }

    default Optional<UUID> findLiveClaimWithNino(String nino) {
        List<UUID> liveClaimsWithNino = findLiveClaimsWithNino(nino);
        if (liveClaimsWithNino.size() > 1) {
            throw new MultipleClaimsWithSameNinoException(liveClaimsWithNino);
        }
        return liveClaimsWithNino.isEmpty() ? Optional.empty() : Optional.of(liveClaimsWithNino.get(0));
    }

    /**
     * finds a claim by reference.
     *
     * @param reference this is newly generated reference to be checked against existing claim for uniqueness.
     * @return claim for given reference.
     */
    Optional<Claim> findByReference(String reference);
}
