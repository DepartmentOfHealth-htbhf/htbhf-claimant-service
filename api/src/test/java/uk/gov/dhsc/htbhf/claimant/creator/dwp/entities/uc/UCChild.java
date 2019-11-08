package uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.BaseEntity;

import java.time.LocalDate;
import javax.persistence.*;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "dwp_uc_child")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class UCChild extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dwp_uc_household_id", nullable = false)
    private UCHousehold household;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

}
