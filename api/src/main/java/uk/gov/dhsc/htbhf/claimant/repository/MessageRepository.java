package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for Message objects.
 */
public interface MessageRepository extends CrudRepository<Message, UUID> {

    @Query("SELECT m "
            + "FROM Message m "
            + "where m.messageType = :messageType and m.messageTimestamp < :olderThan "
            + "order by m.messageTimestamp")
    List<Message> findAllMessagesByTypeOlderThan(
            @Param("messageType") MessageType messageType,
            @Param("olderThan") LocalDateTime olderThan,
            Pageable pageable);

    default List<Message> findAllMessagesDueForProcessingByType(
            @Param("messageType") MessageType messageType, Pageable pageable) {
        return findAllMessagesByTypeOlderThan(messageType, LocalDateTime.now(), pageable);
    }

}
