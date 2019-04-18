package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.dhsc.htbhf.claimant.entity.Message;

import java.util.UUID;

/**
 * JPA repository for Message objects.
 */
public interface MessageRepository extends CrudRepository<Message, UUID> {
}
