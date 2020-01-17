package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class EligibilityOverride {

    @Column(name = "eligibility_override_outcome")
    @Enumerated(EnumType.STRING)
    private EligibilityOutcome eligibilityOutcome;

}