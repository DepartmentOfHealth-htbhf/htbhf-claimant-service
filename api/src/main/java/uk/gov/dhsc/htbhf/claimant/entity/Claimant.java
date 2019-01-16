package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Domain object for a Claimant.
 */
@Entity
@Data
@Builder
@Table(name = "claimant")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Claimant {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @Size(max = 500)
    @Column(name = "first_name")
    private String firstName;

    @NotNull
    @Size(min = 1, max = 500)
    @Column(name = "last_name")
    private String lastName;

    @Column(name = "nino")
    private String nino;
}
