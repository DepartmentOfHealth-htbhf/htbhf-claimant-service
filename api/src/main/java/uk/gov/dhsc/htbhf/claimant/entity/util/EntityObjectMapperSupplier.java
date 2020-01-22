package uk.gov.dhsc.htbhf.claimant.entity.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.util.ObjectMapperSupplier;

import static uk.gov.dhsc.htbhf.ObjectMapperFactory.configureObjectMapper;

/**
 * Supplies an {@link ObjectMapper} that is used by the json hibernate type.
 * See https://vladmihalcea.com/hibernate-types-customize-jackson-objectmapper/.
 * This class is referenced in the hibernate-types.properties file.
 */
public class EntityObjectMapperSupplier implements ObjectMapperSupplier {

    @Override
    public ObjectMapper get() {
        return configureObjectMapper();
    }
}