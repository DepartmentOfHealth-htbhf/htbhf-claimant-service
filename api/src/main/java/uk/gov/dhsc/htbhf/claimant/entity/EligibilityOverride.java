package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;
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

    //TODO custom validation to handle null - AFHS-578
    @Column(name = "eligibility_override_outcome")
    @Enumerated(EnumType.STRING)
    private EligibilityOutcome eligibilityOutcome;

    @Column(name = "eligibility_override_until")
    private LocalDate overrideUntil;

}