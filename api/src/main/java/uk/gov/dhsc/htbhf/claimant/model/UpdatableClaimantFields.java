package uk.gov.dhsc.htbhf.claimant.model;

import lombok.Getter;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumeration of all the fields on a Claimant that may be updated by the claimant.
 */
public enum UpdatableClaimantFields {

    EXPECTED_DELIVERY_DATE("expectedDeliveryDate"),
    ADDRESS("address") {

        @Override
        boolean valueIsDifferent(Claimant originalClaimant, Claimant newClaimant) throws InvocationTargetException, IllegalAccessException {
            return hasAddressChanged(originalClaimant, newClaimant);
        }

        @Override
        void updateOriginal(Claimant originalClaimant, Claimant newClaimant) {
            updateAddress(originalClaimant, newClaimant);
        }
    };

    @Getter
    private final String fieldName;
    private final Method setter;
    private final Method getter;

    /**
     * Constructor that accepts the field name and uses it to obtain the getter and setter methods.
     * @param fieldName the name of the field.
     */
    UpdatableClaimantFields(String fieldName) {
        this.fieldName = fieldName;
        Map<String, Method> methodMap = Arrays.stream(Claimant.class.getDeclaredMethods()).collect(Collectors.toMap(
                method -> method.getName().toLowerCase(Locale.getDefault()),
                method -> method
        ));
        this.setter = methodMap.get("set" + fieldName.toLowerCase(Locale.getDefault()));
        this.getter = methodMap.get("get" + fieldName.toLowerCase(Locale.getDefault()));
    }

    /**
     * Updates the field in the originalObject if the newObject has a value that differs.
     * @param originalObject the object to update.
     * @param newObject the object with the value to update to.
     * @return The name of the field if updated, empty otherwise.
     */
    public Optional<String> updateOriginalIfDifferent(Claimant originalObject, Claimant newObject) {
        try {
            if (valueIsDifferent(originalObject, newObject)) {
                updateOriginal(originalObject, newObject);
                return Optional.of(getFieldName());
            }
        } catch (InvocationTargetException | IllegalAccessException | RuntimeException e) {
            throw new IllegalStateException("Cannot update field " + getFieldName(), e);
        }
        return Optional.empty();
    }

    boolean valueIsDifferent(Claimant originalClaimant, Claimant newClaimant) throws InvocationTargetException, IllegalAccessException {
        Object originalValue = getter.invoke(originalClaimant);
        Object newValue = getter.invoke(newClaimant);
        return !Objects.equals(originalValue, newValue);
    }

    void updateOriginal(Claimant originalClaimant, Claimant newClaimant) throws InvocationTargetException, IllegalAccessException {
        Object newValue = getter.invoke(newClaimant);
        setter.invoke(originalClaimant, newValue);
    }

    private static boolean hasAddressChanged(Claimant originalClaimant, Claimant newClaimant) throws InvocationTargetException, IllegalAccessException {
        Address originalAddress = originalClaimant.getAddress();
        Address newAddress = newClaimant.getAddress();
        if (originalAddress == null && newAddress == null) {
            return false;
        }
        if (originalAddress == null || newAddress == null) {
            return true;
        }
        return !getAddressString(originalAddress).equals(getAddressString(newAddress));
    }

    private static String getAddressString(Address address) {
        return address.getAddressLine1() + ":" + address.getAddressLine2() + ":" + address.getTownOrCity() + ":" + address.getPostcode();
    }

    private static void updateAddress(Claimant originalClaimant, Claimant newClaimant) {
        Address originalAddress = originalClaimant.getAddress();
        Address newAddress = newClaimant.getAddress();
        if (originalAddress == null || newAddress == null) {
            originalClaimant.setAddress(newAddress);
        } else {
            originalAddress.setAddressLine1(newAddress.getAddressLine1());
            originalAddress.setAddressLine2(newAddress.getAddressLine2());
            originalAddress.setTownOrCity(newAddress.getTownOrCity());
            originalAddress.setPostcode(newAddress.getPostcode());
        }
    }
}
