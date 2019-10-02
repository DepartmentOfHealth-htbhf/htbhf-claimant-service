package uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.BaseEntity;

import javax.persistence.*;

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

    @Column(name = "nino")
    private String nino;

    @Column(name = "forename")
    private String forename;

    @Column(name = "surname")
    private String surname;

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "address_town_or_city")
    private String townOrCity;

    @Column(name = "address_postcode")
    private String postcode;

}
