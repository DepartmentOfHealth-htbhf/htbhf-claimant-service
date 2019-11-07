package uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.BaseEntity;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import javax.validation.constraints.Size;

import static java.util.Collections.unmodifiableSet;

@Entity
@Data
@Builder(toBuilder = true)
@Table(name = "dwp_uc_household")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.DataClass")
public class UCHousehold extends BaseEntity {

    @Size(min = 1, max = 50)
    @Column(name = "household_identifier")
    private String householdIdentifier;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "household", orphanRemoval = true)
    @ToString.Exclude
    private final Set<UCAdult> adults = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "household", orphanRemoval = true)
    @ToString.Exclude
    private final Set<UCChild> children = new HashSet<>();

    public UCHousehold addAdult(UCAdult adult) {
        adult.setHousehold(this);
        this.adults.add(adult);
        return this;
    }

    public Set<UCAdult> getAdults() {
        return unmodifiableSet(adults);
    }

    public void setAdults(Set<UCAdult> adults) {
        this.adults.clear();
        adults.forEach(this::addAdult);
    }

    public UCHousehold addChild(UCChild child) {
        child.setHousehold(this);
        this.children.add(child);
        return this;
    }

    public Set<UCChild> getChildren() {
        return unmodifiableSet(children);
    }

    public void setChildren(Set<UCChild> children) {
        this.children.clear();
        children.forEach(this::addChild);
    }
}
