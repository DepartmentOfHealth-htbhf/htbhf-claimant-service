package uk.gov.dhsc.htbhf.claimant.creator.dwp.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;

@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class Child extends BaseEntity {

    @Column(name = "forename")
    private String forename;

    @Column(name = "surname")
    private String surname;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

}
