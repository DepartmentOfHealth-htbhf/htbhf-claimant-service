package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffIgnore;

import javax.persistence.*;

@Data
@NoArgsConstructor
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public abstract class VersionedEntity extends BaseEntity {

    @Version
    @Column(name = "version_number")
    @DiffIgnore
    private Integer versionNumber;
}
