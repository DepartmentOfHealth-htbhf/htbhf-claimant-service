package uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.legacy;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.BaseEntity;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.Household;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import static java.util.Collections.unmodifiableSet;

@Entity
@Data
@Builder(toBuilder = true)
@Table(name = "dwp_legacy_household")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.DataClass")
public class LegacyHousehold extends BaseEntity implements Household {

    @Column(name = "household_identifier")
    private String householdIdentifier;

    @Column(name = "file_import_number")
    private Integer fileImportNumber;

    @Column(name = "award")
    private String award;

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "address_town_or_city")
    private String townOrCity;

    @Column(name = "address_postcode")
    private String postcode;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "household", orphanRemoval = true)
    @ToString.Exclude
    private final Set<LegacyAdult> adults = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "household", orphanRemoval = true)
    @ToString.Exclude
    private final Set<LegacyChild> children = new HashSet<>();

    public LegacyHousehold addAdult(LegacyAdult adult) {
        adult.setHousehold(this);
        this.adults.add(adult);
        return this;
    }

    public Set<LegacyAdult> getAdults() {
        return unmodifiableSet(adults);
    }

    public void setAdults(Set<LegacyAdult> adults) {
        this.adults.clear();
        adults.forEach(this::addAdult);
    }

    public LegacyHousehold addChild(LegacyChild child) {
        child.setHousehold(this);
        this.children.add(child);
        return this;
    }

    @Override
    public Set<LegacyChild> getChildren() {
        return unmodifiableSet(children);
    }

    public void setChildren(Set<LegacyChild> children) {
        this.children.clear();
        children.forEach(this::addChild);
    }

}
