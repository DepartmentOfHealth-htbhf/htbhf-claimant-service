package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.util.UUID;

/**
 * JPA repository for {@link PaymentCycle}s.
 */
public interface PaymentCycleRepository extends CrudRepository<PaymentCycle, UUID> {
}
