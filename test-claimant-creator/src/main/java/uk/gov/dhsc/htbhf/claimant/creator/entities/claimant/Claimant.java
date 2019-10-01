package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.*;

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

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "nino")
    private String nino;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "expected_delivery_date")
    // no validation here as we don't want to prevent re-saving a claimant 6 months after initially created
    private LocalDate expectedDeliveryDate;

    @OneToOne(cascade = CascadeType.ALL)
    private Address address;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "children_dob_json")
    @Type(type = "json")
    private List<LocalDate> childrenDob;
}
