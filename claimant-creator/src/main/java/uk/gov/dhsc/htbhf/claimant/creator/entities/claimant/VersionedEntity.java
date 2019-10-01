package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public abstract class VersionedEntity extends BaseEntity {

    @Version
    @Column(name = "version_number")
    private Integer versionNumber;
}
