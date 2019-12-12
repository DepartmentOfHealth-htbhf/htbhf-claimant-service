package uk.gov.dhsc.htbhf.claimant.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.aValidAddress;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.aValidAddressBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.*;

class UpdatableClaimantFieldTest {

    @ParameterizedTest
    @EnumSource(UpdatableClaimantField.class)
    void shouldReportFieldsHaveDifferentValues(UpdatableClaimantField field) {
        Claimant originalClaimant = Claimant.builder().build();
        Claimant newClaimant = aValidClaimantBuilder()
                .expectedDeliveryDate(LocalDate.now())
                .initiallyDeclaredChildrenDob(singletonList(LocalDate.now()))
                .build();

        assertThat(field.valueIsDifferent(originalClaimant, newClaimant)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(UpdatableClaimantField.class)
    void shouldReportFieldsDoNotHaveDifferentValues(UpdatableClaimantField field) {
        LocalDate expectedDeliveryDate = LocalDate.now();
        Claimant originalClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        assertThat(newClaimant).isNotEqualTo(originalClaimant);

        assertThat(field.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(UpdatableClaimantField.class)
    void shouldReportNullFieldsDoNotHaveDifferentValues(UpdatableClaimantField field) {
        Claimant originalClaimant = Claimant.builder().build();
        Claimant newClaimant = Claimant.builder().build();
        assertThat(newClaimant).isNotEqualTo(originalClaimant);

        assertThat(field.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
    }

    @Test
    void shouldUpdateFirstName() {
        Claimant originalClaimant = aValidClaimant();
        Claimant newClaimant = aClaimantWithFirstName("MyNewName");

        UpdatableClaimantField.FIRST_NAME.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalClaimant.getFirstName()).isEqualTo(newClaimant.getFirstName());
        assertThat(originalClaimant).isNotEqualTo(newClaimant);
    }

    @Test
    void shouldUpdateLastName() {
        Claimant originalClaimant = aValidClaimant();
        Claimant newClaimant = aClaimantWithLastName("MyNewName");

        UpdatableClaimantField.LAST_NAME.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalClaimant.getLastName()).isEqualTo(newClaimant.getLastName());
    }

    @Test
    void shouldUpdateDateOfBirth() {
        Claimant originalClaimant = aValidClaimant();
        Claimant newClaimant = aClaimantWithDateOfBirth(LocalDate.now().minusYears(18));

        UpdatableClaimantField.DATE_OF_BIRTH.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalClaimant.getDateOfBirth()).isEqualTo(newClaimant.getDateOfBirth());
    }

    @Test
    void shouldUpdateExpectedDeliveryDate() {
        Claimant originalClaimant = aClaimantWithExpectedDeliveryDate(LocalDate.now().minusDays(1));
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(LocalDate.now());
        assertThat(newClaimant).isNotEqualTo(originalClaimant);

        UpdatableClaimantField.EXPECTED_DELIVERY_DATE.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalClaimant.getExpectedDeliveryDate()).isEqualTo(newClaimant.getExpectedDeliveryDate());
    }

    @Test
    void shouldUpdatePhoneNumber() {
        Claimant originalClaimant = aValidClaimant();
        Claimant newClaimant = aClaimantWithPhoneNumber("+44987654321");
        assertThat(newClaimant).isNotEqualTo(originalClaimant);

        UpdatableClaimantField.PHONE_NUMBER.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalClaimant.getPhoneNumber()).isEqualTo(newClaimant.getPhoneNumber());
    }

    @Test
    void shouldUpdateChildrenDob() {
        Claimant originalClaimant = aClaimantWithChildrenDob(LocalDate.now().minusYears(2));
        Claimant newClaimant = aClaimantWithChildrenDob(LocalDate.now().minusYears(2), LocalDate.now().minusMonths(1));
        assertThat(newClaimant).isNotEqualTo(originalClaimant);

        UpdatableClaimantField.CHILDREN_DOB.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalClaimant.getInitiallyDeclaredChildrenDob()).isEqualTo(newClaimant.getInitiallyDeclaredChildrenDob());
    }

    @Test
    void shouldReportAddressIsDifferentForSingleAddressField() {
        Claimant originalClaimant = aValidClaimantBuilder()
                .address(aValidAddress())
                .build();
        Claimant newClaimant = aValidClaimantBuilder()
                .address(aValidAddressBuilder().postcode("ZZ9 9XX").build())
                .build();

        assertThat(UpdatableClaimantField.ADDRESS.valueIsDifferent(originalClaimant, newClaimant)).isTrue();
    }

    @Test
    void shouldUpdateAllAddressFields() {
        Address originalAddress = Address.builder().build();
        Claimant originalClaimant = aValidClaimantBuilder()
                .address(originalAddress)
                .build();
        Address newAddress = aValidAddress();
        Claimant newClaimant = aValidClaimantBuilder()
                .address(newAddress)
                .build();

        UpdatableClaimantField.ADDRESS.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalAddress.getAddressLine1()).isEqualTo(newAddress.getAddressLine1());
        assertThat(originalAddress.getAddressLine2()).isEqualTo(newAddress.getAddressLine2());
        assertThat(originalAddress.getTownOrCity()).isEqualTo(newAddress.getTownOrCity());
        assertThat(originalAddress.getPostcode()).isEqualTo(newAddress.getPostcode());
        assertThat(originalAddress.getId()).isNotEqualTo(newAddress.getId());
    }
}