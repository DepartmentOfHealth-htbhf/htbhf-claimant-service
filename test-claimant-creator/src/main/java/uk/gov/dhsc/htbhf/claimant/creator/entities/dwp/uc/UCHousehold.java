package uk.gov.dhsc.htbhf.claimant.creator.entities.dwp.uc;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.creator.entities.dwp.BaseEntity;
import uk.gov.dhsc.htbhf.claimant.creator.entities.dwp.Household;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import static java.util.Collections.unmodifiableSet;

@Entity
@Data
@Builder(toBuilder = true)
@Table(name = "dwp_uc_household")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.DataClass")
public class UCHousehold extends BaseEntity implements Household {

    @Column(name = "household_identifier")
    private String householdIdentifier;

    @Column(name = "file_import_number")
    private Integer fileImportNumber;

    @Column(name = "award_date")
    private LocalDate awardDate;

    @Column(name = "last_assessment_period_start")
    private LocalDate lastAssessmentPeriodStart;

    @Column(name = "last_assessment_period_end")
    private LocalDate lastAssessmentPeriodEnd;

    @Column(name = "household_member_pregnant")
    private Boolean householdMemberPregnant;

    @Column(name = "earnings_threshold_exceeded")
    private Boolean earningsThresholdExceeded;

    @Column(name = "no_of_children_under_four")
    private Integer childrenUnderFour;

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

    @Override
    public Set<UCChild> getChildren() {
        return unmodifiableSet(children);
    }

    public void setChildren(Set<UCChild> children) {
        this.children.clear();
        children.forEach(this::addChild);
    }
}
