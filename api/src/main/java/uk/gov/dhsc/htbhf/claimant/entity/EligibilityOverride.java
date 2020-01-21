package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import uk.gov.dhsc.htbhf.claimant.model.constraint.ListOfDatesInPast;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import static uk.gov.dhsc.htbhf.claimant.entity.BaseEntity.JSON_TYPE;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class EligibilityOverride {

    @Column(name = "eligibility_override_outcome")
    @Enumerated(EnumType.STRING)
    private EligibilityOutcome eligibilityOutcome;

    @Column(name = "eligibility_override_until")
    private LocalDate overrideUntil;

    @Column(name = "eligibility_override_children_dob")
    @Type(type = JSON_TYPE)
    @ListOfDatesInPast(message = "dates of birth of children should be all in the past")
    private List<LocalDate> childrenDob;

}