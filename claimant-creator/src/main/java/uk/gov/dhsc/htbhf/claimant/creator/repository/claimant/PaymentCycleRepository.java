package uk.gov.dhsc.htbhf.claimant.creator.repository.claimant;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.creator.entities.claimant.PaymentCycle;

import java.util.UUID;

/**
 * JPA repository for {@link PaymentCycle}s.
 */
public interface PaymentCycleRepository extends CrudRepository<PaymentCycle, UUID> {
}
