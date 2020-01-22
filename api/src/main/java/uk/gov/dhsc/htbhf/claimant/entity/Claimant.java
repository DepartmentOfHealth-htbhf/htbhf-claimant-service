package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import org.hibernate.annotations.Type;
import uk.gov.dhsc.htbhf.claimant.model.constraint.ListOfDatesInPast;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static uk.gov.dhsc.htbhf.claimant.entity.BaseEntity.JSON_TYPE;
import static uk.gov.dhsc.htbhf.claimant.model.Constants.VALID_EMAIL_REGEX;

/**
 * Domain object for a Claimant.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Embeddable
public class Claimant {

    @NotNull
    @Size(min = 1, max = 500)
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

    @NotNull
    @Pattern(regexp = VALID_EMAIL_REGEX, message = "invalid email address")
    @Size(max = 256)
    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "initially_declared_children_dob")
    @Type(type = JSON_TYPE)
    @ListOfDatesInPast(message = "dates of birth of children should be all in the past")
    private List<LocalDate> initiallyDeclaredChildrenDob;
}
