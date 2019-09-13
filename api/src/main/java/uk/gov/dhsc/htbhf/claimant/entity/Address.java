package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.model.constraint.NotPattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static uk.gov.dhsc.htbhf.claimant.regex.PostcodeRegex.CHANNEL_ISLAND_POST_CODE_REGEX;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public class Address extends VersionedEntity {

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

    @Size(max = 500)
    @Column(name = "county")
    private String county;

    @NotNull
    @Pattern(regexp = UK_POST_CODE_REGEX, message = "invalid postcode format")
    @NotPattern(regexp = CHANNEL_ISLAND_POST_CODE_REGEX, message = "postcodes in the Channel Islands or Isle of Man are not acceptable")
    @Column(name = "postcode")
    private String postcode;
}
