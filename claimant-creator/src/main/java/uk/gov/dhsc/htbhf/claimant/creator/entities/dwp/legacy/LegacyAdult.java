package uk.gov.dhsc.htbhf.claimant.creator.entities.dwp.legacy;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.creator.entities.dwp.BaseEntity;

import javax.persistence.*;

@Entity
@Data
@Builder
@Table(name = "dwp_legacy_adult")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class LegacyAdult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dwp_legacy_household_id", nullable = false)
    private LegacyHousehold household;

    @Column(name = "nino")
    private String nino;

    @Column(name = "forename")
    private String forename;

    @Column(name = "surname")
    private String surname;

}
