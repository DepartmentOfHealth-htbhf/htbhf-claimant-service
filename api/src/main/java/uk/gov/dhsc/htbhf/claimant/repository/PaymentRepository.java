package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;

import java.util.UUID;

public interface PaymentRepository extends CrudRepository<Payment, UUID> {
}