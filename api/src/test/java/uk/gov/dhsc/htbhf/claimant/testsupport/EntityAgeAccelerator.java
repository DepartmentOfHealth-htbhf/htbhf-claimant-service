package uk.gov.dhsc.htbhf.claimant.testsupport;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.BaseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Capable of accelerating the ageing of entities in the database, by modifying all timestamp properties to put them further in the past.
 */
@Component
@Slf4j
public class EntityAgeAccelerator {

    private static final String BASE_PACKAGE_NAME = "uk.gov.dhsc.htbhf";
    private static final Map<Class, List<Field>> TEMPORAL_FIELDS = new HashMap<>();
    private static final Map<Class, List<Field>> TEMPORAL_LISTS = new HashMap<>();
    private static final Map<Class, List<Field>> NON_ENTITY_CHILD_OBJECT_FIELDS = new HashMap<>();
    private static final Map<Class, List<Field>> NON_ENTITY_CHILD_OBJECT_LISTS = new HashMap<>();

    /**
     * Adjusts all LocalDate and LocalDateTime fields on the given entity by subtracting the given number of days, and on all child objects of that entity.
     * A 'child object' is considered to be the value of any field whose type is in a package starting 'uk.gov.dhsc.htbhf',
     * excluding enums and objects extending {@link BaseEntity} (to prevent infinite recursion & entities being aged twice).
     *
     * @param objectToAge  the object to be aged.
     * @param numberOfDays the number of days to age by.
     */
    public static void ageObject(Object objectToAge, int numberOfDays) {
        if (objectToAge == null) {
            return;
        }
        // if the entity is proxied we cannot compare class name or hierarchy
        Object entity = Hibernate.unproxy(objectToAge);
        if (entity instanceof BaseEntity) {
            log.info("Fast-forwarding {} {} by {} days", entity.getClass().getSimpleName(), ((BaseEntity) entity).getId(), numberOfDays);
        }
        if (entity instanceof uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.BaseEntity) {
            log.info("Fast-forwarding {} {} by {} days",
                    entity.getClass().getSimpleName(),
                    ((uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.BaseEntity) entity).getId(),
                    numberOfDays);
        }
        adjustTemporalFields(entity, numberOfDays);
        adjustTemporalLists(entity, numberOfDays);

        List<Object> childObjects = getChildObjectsToUpdate(entity);
        childObjects.forEach(child -> ageObject(child, numberOfDays));
    }

    private static void adjustTemporalFields(Object entity, int numberOfDays) {
        List<Field> fields = TEMPORAL_FIELDS.computeIfAbsent(entity.getClass(), entityClass -> getFieldsOfType(entityClass, Temporal.class));
        fields.forEach(field -> {
            try {
                Temporal date = (Temporal) field.get(entity);
                if (date != null) {
                    Temporal newDate = date.minus(numberOfDays, ChronoUnit.DAYS);
                    field.set(entity, newDate);
                    log.debug("{}.{}: {} -> {}", entity.getClass().getSimpleName(), field.getName(), date, newDate);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void adjustTemporalLists(Object entity, int numberOfDays) {
        List<Field> fields = TEMPORAL_LISTS.computeIfAbsent(entity.getClass(), entityClass -> getListFieldsOfType(entityClass, Temporal.class));
        fields.forEach(field -> {
            try {
                List<Temporal> dates = (List<Temporal>) field.get(entity);
                if (!CollectionUtils.isEmpty(dates)) {
                    List<Temporal> newDates = dates.stream().map(localDate -> localDate.minus(numberOfDays, ChronoUnit.DAYS)).collect(Collectors.toList());
                    field.set(entity, newDates);
                    log.debug("{}.{}: {} -> {}", entity.getClass().getSimpleName(), field.getName(), dates, newDates);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static List<Field> getFieldsOfType(Class entityClass, Class fieldType) {
        List<Field> matching = new ArrayList<>();
        List<Field> fields = getDeclaredFields(entityClass);
        for (Field field : fields) {
            if (fieldType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                matching.add(field);
            }
        }
        return matching;
    }

    private static List<Field> getListFieldsOfType(Class entityClass, Class fieldType) {
        List<Field> matching = new ArrayList<>();
        List<Field> fields = getDeclaredFields(entityClass);
        for (Field field : fields) {
            if (List.class.isAssignableFrom(field.getType())) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                Class<?> actualClass = (Class<?>) listType.getActualTypeArguments()[0];
                if (fieldType.isAssignableFrom(actualClass)) {
                    field.setAccessible(true);
                    matching.add(field);
                }
            }
        }
        return matching;
    }

    private static List<Object> getChildObjectsToUpdate(Object entity) {
        List<Field> fields = NON_ENTITY_CHILD_OBJECT_FIELDS.computeIfAbsent(entity.getClass(), EntityAgeAccelerator::getNonEntityFieldsInHtbhfPackage);
        List<Field> lists = NON_ENTITY_CHILD_OBJECT_LISTS.computeIfAbsent(entity.getClass(), EntityAgeAccelerator::getNonEntityListsInHtbhfPackage);
        List<Object> children = new ArrayList<>();
        fields.forEach(field -> {
            try {
                children.add(field.get(entity));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        lists.forEach(field -> {
            try {
                List<Object> list = (List<Object>) field.get(entity);
                if (!CollectionUtils.isEmpty(list)) {
                    children.addAll(list);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        return children;
    }

    private static List<Field> getNonEntityFieldsInHtbhfPackage(Class entityClass) {
        List<Field> matching = new ArrayList<>();
        List<Field> fields = getDeclaredFields(entityClass);
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            if (fieldType.getName().startsWith(BASE_PACKAGE_NAME)
                    && !BaseEntity.class.isAssignableFrom(fieldType)
                    && !fieldType.isEnum()
                    && !Modifier.isStatic(field.getModifiers())
            ) {
                field.setAccessible(true);
                matching.add(field);
            }
        }
        return matching;
    }

    private static List<Field> getNonEntityListsInHtbhfPackage(Class entityClass) {
        List<Field> matching = new ArrayList<>();
        List<Field> fields = getDeclaredFields(entityClass);
        for (Field field : fields) {
            if (List.class.isAssignableFrom(field.getType()) && !Modifier.isStatic(field.getModifiers())) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                Class<?> actualClass = (Class<?>) listType.getActualTypeArguments()[0];
                if (actualClass.getName().startsWith(BASE_PACKAGE_NAME) && !BaseEntity.class.isAssignableFrom(actualClass)) {
                    field.setAccessible(true);
                    matching.add(field);
                }
            }
        }
        return matching;
    }


    private static List<Field> getDeclaredFields(Class entityClass) {
        List<Field> fields = new ArrayList<>();
        Field[] declaredFields = entityClass.getDeclaredFields();
        fields.addAll(Arrays.asList(declaredFields));
        if (entityClass.getSuperclass() != null) {
            fields.addAll(getDeclaredFields(entityClass.getSuperclass()));
        }
        return fields;
    }

}
