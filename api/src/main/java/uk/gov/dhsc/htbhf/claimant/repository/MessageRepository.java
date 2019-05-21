package uk.gov.dhsc.htbhf.claimant.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for Message objects.
 */
public interface MessageRepository extends CrudRepository<Message, UUID> {

    @Query("SELECT m "
            + "FROM Message m "
            + "where m.messageType = :messageType "
            + "order by m.messageTimestamp")
    List<Message> findAllMessagesByTypeOrderedByDate(@Param("messageType") MessageType messageType, Pageable pageable);

}
