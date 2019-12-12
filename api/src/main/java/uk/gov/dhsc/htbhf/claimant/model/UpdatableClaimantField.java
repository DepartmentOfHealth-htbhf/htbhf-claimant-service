package uk.gov.dhsc.htbhf.claimant.model;

import lombok.Getter;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.util.Objects;

/**
 * Enumeration of all the fields on a Claimant that may be updated by the claimant.
 */
@Getter
public enum UpdatableClaimantField {

    FIRST_NAME("firstName") {
        @Override
        Object getValue(Claimant claimant) {
            return claimant.getFirstName();
        }

        @Override
        public void updateOriginal(Claimant originalClaimant, Claimant newClaimant) {
            originalClaimant.setFirstName(newClaimant.getFirstName());
        }
    },
    LAST_NAME("lastName") {
        @Override
        Object getValue(Claimant claimant) {
            return claimant.getLastName();
        }

        @Override
        public void updateOriginal(Claimant originalClaimant, Claimant newClaimant) {
            originalClaimant.setLastName(newClaimant.getLastName());
        }
    },
    DATE_OF_BIRTH("dateOfBirth") {
        @Override
        Object getValue(Claimant claimant) {
            return claimant.getDateOfBirth();
        }

        @Override
        public void updateOriginal(Claimant originalClaimant, Claimant newClaimant) {
            originalClaimant.setDateOfBirth(newClaimant.getDateOfBirth());
        }
    },
    EXPECTED_DELIVERY_DATE("expectedDeliveryDate") {
        @Override
        Object getValue(Claimant claimant) {
            return claimant.getExpectedDeliveryDate();
        }

        @Override
        public void updateOriginal(Claimant originalClaimant, Claimant newClaimant) {
            originalClaimant.setExpectedDeliveryDate(newClaimant.getExpectedDeliveryDate());
        }
    },
    PHONE_NUMBER("phoneNumber") {
        @Override
        Object getValue(Claimant claimant) {
            return claimant.getPhoneNumber();
        }

        @Override
        public void updateOriginal(Claimant originalClaimant, Claimant newClaimant) {
            originalClaimant.setPhoneNumber(newClaimant.getPhoneNumber());
        }
    },
    CHILDREN_DOB("childrenDob") {
        @Override
        Object getValue(Claimant claimant) {
            return claimant.getInitiallyDeclaredChildrenDob();
        }

        @Override
        public void updateOriginal(Claimant originalClaimant, Claimant newClaimant) {
            originalClaimant.setInitiallyDeclaredChildrenDob(newClaimant.getInitiallyDeclaredChildrenDob());
        }
    },
    ADDRESS("address") {
        @Override
        public boolean valueIsDifferent(Claimant originalClaimant, Claimant newClaimant) {
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

        private String getAddressString(Address address) {
            return address.getAddressLine1() + ":" + address.getAddressLine2() + ":" + address.getTownOrCity() + ":" + address.getPostcode();
        }

        @Override
        public void updateOriginal(Claimant originalClaimant, Claimant newClaimant) {
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

        @Override
        Object getValue(Claimant claimant) {
            return claimant.getAddress();
        }
    };

    private final String fieldName;

    UpdatableClaimantField(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Checks whether the value of the relevant field differs between the two objects.
     *
     * @param originalClaimant the original claimant.
     * @param newClaimant      the new claimant.
     * @return true if the new claimant has a different value to the original.
     */
    public boolean valueIsDifferent(Claimant originalClaimant, Claimant newClaimant) {
        Object originalValue = getValue(originalClaimant);
        Object newValue = getValue(newClaimant);
        return !Objects.equals(originalValue, newValue);
    }

    /**
     * Updates the field value of the original to be the same as the new claimant.
     *
     * @param originalClaimant the original claimant.
     * @param newClaimant      the new claimant.
     */
    public abstract void updateOriginal(Claimant originalClaimant, Claimant newClaimant);

    abstract Object getValue(Claimant claimant);
}
