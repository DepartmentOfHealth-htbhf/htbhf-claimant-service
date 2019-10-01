package uk.gov.dhsc.htbhf.claimant.creator.entities.dwp.legacy;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.creator.entities.dwp.Child;

import javax.persistence.*;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "dwp_legacy_child")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class LegacyChild extends Child {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dwp_legacy_household_id", nullable = false)
    private LegacyHousehold household;
}
