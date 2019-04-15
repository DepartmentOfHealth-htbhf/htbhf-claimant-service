package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

class VoucherEntitlementToDTOConverterTest {

    private VoucherEntitlementToDTOConverter converter = new VoucherEntitlementToDTOConverter();

    @Test
    void shouldConvert() {
        //Given
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        //When
        VoucherEntitlementDTO converted = converter.convert(voucherEntitlement);
        //Then
        assertThat(converted).isEqualTo(aValidVoucherEntitlementDTO());
    }

    @Test
    void shouldFailToConvertANullObject() {
        IllegalArgumentException thrown = catchThrowableOfType(() -> converter.convert(null), IllegalArgumentException.class);
        assertThat(thrown).hasMessage("VoucherEntitlement must not be null");
    }
}
