package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static uk.gov.dhsc.htbhf.claimant.regex.PostcodeRegex.UK_POST_CODE_REGEX;

/**
 * Domain object for an Address.
 */
@Entity
@Data
@Builder
@Table(name = "address")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Address {

    @Id
    @Getter(AccessLevel.NONE)
    @Access(AccessType.PROPERTY)
    private UUID id;

    @NotNull
    @Size(min = 1, max = 500)
    @Column(name = "address_line_1")
    private String addressLine1;

    @Size(max = 500)
    @Column(name = "address_line_2")
    private String addressLine2;

    @NotNull
    @Size(min = 1, max = 500)
    @Column(name = "town_or_city")
    private String townOrCity;

    @NotNull
    @Pattern(regexp = UK_POST_CODE_REGEX, message = "invalid postcode format")
    @Column(name = "postcode")
    private String postcode;

    /**
     * Adding a custom getter for the id so that we can compare an Address object before and after its initial
     * persistence and they will be the same.
     *
     * @return The id for the Address.
     */
    public UUID getId() {
        if (id == null) {
            this.id = UUID.randomUUID();
        }
        return this.id;
    }
}
