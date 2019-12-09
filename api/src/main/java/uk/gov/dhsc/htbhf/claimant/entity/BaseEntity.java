package uk.gov.dhsc.htbhf.claimant.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import java.util.UUID;
import javax.persistence.*;

@Data
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@MappedSuperclass
@TypeDefs({
        @TypeDef(name = BaseEntity.JSON_TYPE, typeClass = JsonBinaryType.class),
})
public abstract class BaseEntity {

    public static final String JSON_TYPE = "json";

    @Id
    @Access(AccessType.PROPERTY)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Adding a custom getter for the id so that we can compare an entity before and after its initial
     * persistence and they will be the same.
     *
     * @return The id for the entity.
     */
    public UUID getId() {
        if (id == null) {
            this.id = UUID.randomUUID();
        }
        return this.id;
    }
}
