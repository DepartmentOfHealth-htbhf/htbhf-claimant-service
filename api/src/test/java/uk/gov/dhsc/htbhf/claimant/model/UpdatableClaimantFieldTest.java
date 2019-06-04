package uk.gov.dhsc.htbhf.claimant.model;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.aValidAddress;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.aValidAddressBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.*;

class UpdatableClaimantFieldTest {

    @Test
    void shouldReportFieldsHaveDifferentValues() {
        Claimant originalClaimant = Claimant.builder().build();
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(LocalDate.now());

        assertThat(UpdatableClaimantField.FIRST_NAME.valueIsDifferent(originalClaimant, newClaimant)).isTrue();
        assertThat(UpdatableClaimantField.LAST_NAME.valueIsDifferent(originalClaimant, newClaimant)).isTrue();
        assertThat(UpdatableClaimantField.DATE_OF_BIRTH.valueIsDifferent(originalClaimant, newClaimant)).isTrue();
        assertThat(UpdatableClaimantField.EXPECTED_DELIVERY_DATE.valueIsDifferent(originalClaimant, newClaimant)).isTrue();
        assertThat(UpdatableClaimantField.ADDRESS.valueIsDifferent(originalClaimant, newClaimant)).isTrue();
    }

    @Test
    void shouldReportFieldsDoNotHaveDifferentValues() {
        LocalDate expectedDeliveryDate = LocalDate.now();
        Claimant originalClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);

        assertThat(UpdatableClaimantField.FIRST_NAME.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
        assertThat(UpdatableClaimantField.LAST_NAME.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
        assertThat(UpdatableClaimantField.DATE_OF_BIRTH.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
        assertThat(UpdatableClaimantField.EXPECTED_DELIVERY_DATE.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
        assertThat(UpdatableClaimantField.ADDRESS.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
    }

    @Test
    void shouldReportNullFieldsDoNotHaveDifferentValues() {
        Claimant originalClaimant = Claimant.builder().build();
        Claimant newClaimant = Claimant.builder().build();

        assertThat(UpdatableClaimantField.FIRST_NAME.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
        assertThat(UpdatableClaimantField.LAST_NAME.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
        assertThat(UpdatableClaimantField.DATE_OF_BIRTH.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
        assertThat(UpdatableClaimantField.EXPECTED_DELIVERY_DATE.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
        assertThat(UpdatableClaimantField.ADDRESS.valueIsDifferent(originalClaimant, newClaimant)).isFalse();
    }

    @Test
    void shouldUpdateFirstName() {
        Claimant originalClaimant = aValidClaimant();
        Claimant newClaimant = aClaimantWithFirstName("MyNewName");

        UpdatableClaimantField.FIRST_NAME.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalClaimant.getFirstName()).isEqualTo(newClaimant.getFirstName());
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

        UpdatableClaimantField.EXPECTED_DELIVERY_DATE.updateOriginal(originalClaimant, newClaimant);

        assertThat(originalClaimant.getExpectedDeliveryDate()).isEqualTo(newClaimant.getExpectedDeliveryDate());
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