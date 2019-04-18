package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;

import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "message_queue")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public class Message extends BaseEntity {

    @NotNull
    @Column(name = "message_timestamp")
    private LocalDateTime messageTimestamp;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType;

    @NotNull
    @Column(name = "message_payload")
    private String messagePayload;

}
