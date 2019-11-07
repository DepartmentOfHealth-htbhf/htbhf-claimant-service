package uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.BaseEntity;

import java.time.LocalDate;
import javax.persistence.*;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Entity
@Data
@Builder
@Table(name = "dwp_uc_adult")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class UCAdult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dwp_uc_household_id", nullable = false)
    private UCHousehold household;

    @Size(max = 9)
    @Column(name = "nino")
    private String nino;

    @Size(max = 500)
    @Column(name = "surname")
    private String surname;

    @Size(max = 500)
    @Column(name = "address_line_1")
    private String addressLine1;

    @Size(max = 10)
    @Column(name = "address_postcode")
    private String postcode;

    @Size(max = 256)
    @Pattern(regexp = "(^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$)")
    @Column(name = "email_address")
    private String emailAddress;

    @Pattern(regexp = "^\\+44\\d{9,10}$")
    @Column(name = "mobile_phone_number")
    private String mobilePhoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

}
