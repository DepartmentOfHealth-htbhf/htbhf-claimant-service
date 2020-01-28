package uk.gov.dhsc.htbhf.claimant.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.Type;
import uk.gov.dhsc.htbhf.claimant.model.constraint.ListOfDatesInPast;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import static uk.gov.dhsc.htbhf.claimant.entity.BaseEntity.JSON_TYPE;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class EligibilityOverride {

    @JsonProperty("overrideOutcome")
    @Enumerated(EnumType.STRING)
    private EligibilityOutcome eligibilityOutcome;

    @JsonProperty("overrideUntil")
    private LocalDate overrideUntil;

    @JsonProperty("childrenDob")
    @Type(type = JSON_TYPE)
    @ListOfDatesInPast(message = "dates of birth of children should be all in the past")
    private List<LocalDate> childrenDob;

}