package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;

import java.time.LocalDate;
import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * Domain object for a Claimant.
 */
@Entity
@Data
@Builder(toBuilder = true)
@Table(name = "claimant")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public class Claimant extends VersionedEntity {

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
    private Address address;

    @NotNull
    @Pattern(regexp = "^\\+44\\d{9,10}$", message = "invalid UK phone number, must be in +447123456789 format")
    @Column(name = "phone_number")
    private String phoneNumber;
}
