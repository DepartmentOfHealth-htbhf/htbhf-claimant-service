package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link PaymentCycle}s.
 */
public interface PaymentCycleRepository extends CrudRepository<PaymentCycle, UUID> {

    /**
     * Looks for all active (ACTIVE, PENDING_EXPIRY) claims whose latest PaymentCycle has an end date on or before cycleEndDate.
     * @param cycleEndDate the date by which the cycle should end.
     * @return a List of {@link ClosingPaymentCycle}s containing the claim id and cycle end date.
     */
    @Query(nativeQuery = true, value =
            "SELECT Cast(claim_id as varchar) as claimIdString, Cast(cycle_id as varchar) as cycleIdString, cycle_end_date as cycleEndDateTimestamp FROM "
                    + "(SELECT DISTINCT ON (c.id) c.id as claim_id, p.cycle_end_date, p.id as cycle_id "
                    + "FROM  claim c inner join payment_cycle p  on p.claim_id = c.id "
                    + "WHERE c.claim_status in ('ACTIVE', 'PENDING_EXPIRY') "
                    + "ORDER BY c.id, p.cycle_end_date DESC NULLS LAST "
                    + ") AS latest_payment "
                    + "WHERE cycle_end_date <= ?1")
    List<ClosingPaymentCycle> findActiveClaimsWithCycleEndingOnOrBefore(LocalDate cycleEndDate);

    /**
     * Returns the current PaymentCycle (the one with the latest cycleEndDate) for the given claim.
     * This auto-implemented method has an unhelpful name and returns a list - use {@link #findCurrentCycleForClaim(Claim)} instead.
     * @param claim the claim to get a PaymentCycle for.
     * @return List containing the desired PaymentCycle. Empty if the given claim has no payment cycles.
     */
    List<PaymentCycle> findFirstByClaimOrderByCycleEndDateDesc(Claim claim);

    /**
     * Returns the current PaymentCycle (the one with the latest cycleEndDate) for the given claim.
     * @param claim the claim to get a PaymentCycle for.
     * @return Optional containing the desired PaymentCycle. Empty if the given claim has no payment cycles.
     */
    default Optional<PaymentCycle> findCurrentCycleForClaim(Claim claim) {
        List<PaymentCycle> cycles = findFirstByClaimOrderByCycleEndDateDesc(claim);
        if (cycles.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(cycles.get(0));
    }
}
