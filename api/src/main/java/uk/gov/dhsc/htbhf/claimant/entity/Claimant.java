package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Domain object for a Claimant.
 */
@Entity
@Data
@Builder(toBuilder = true)
@Table(name = "claimant")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Claimant {

    @Id
    @Getter(AccessLevel.NONE)
    @Access(AccessType.PROPERTY)
    private UUID id;

    @Size(max = 500)
    @Column(name = "first_name")
    private String firstName;

    @NotNull
    @Size(min = 1, max = 500)
    @Column(name = "last_name")
    private String lastName;

    @NotNull
    @Pattern(regexp = "[a-zA-Z]{2}\\d{6}[a-dA-D]")
    @Column(name = "nino")
    private String nino;

    @NotNull
    @Past
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "expected_delivery_date")
    // no validation here as we don't want to prevent re-saving a claimant 6 months after initially created
    private LocalDate expectedDeliveryDate;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL)
    private Address cardDeliveryAddress;

    /**
     * Adding a custom getter for the id so that we can compare a Claimant object before and after its initial
     * persistence and they will be the same.
     *
     * @return The id for the Claimant.
     */
    public UUID getId() {
        if (id == null) {
            this.id = UUID.randomUUID();
        }
        return this.id;
    }
}
