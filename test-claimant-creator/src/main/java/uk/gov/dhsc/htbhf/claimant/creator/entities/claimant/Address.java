package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "town_or_city")
    private String townOrCity;

    @Column(name = "county")
    private String county;

    @Column(name = "postcode")
    private String postcode;
}
