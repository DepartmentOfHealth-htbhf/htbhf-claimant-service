package uk.gov.dhsc.htbhf.claimant.entity.converter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringBootTest
class VoucherEntitlementConverterTest {

    @MockBean
    private ObjectMapper objectMapper;

    @Autowired
    private VoucherEntitlementConverter converter;

    @Test
    void shouldConvertVoucherEntitlementToString() throws JsonProcessingException {
        String voucherEntitlementJson = "{pregnancyVoucher: 1}";
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().build();
        given(objectMapper.writeValueAsString(any())).willReturn(voucherEntitlementJson);

        String result = converter.convertToDatabaseColumn(voucherEntitlement);

        assertThat(result).isEqualTo(voucherEntitlementJson);
        verify(objectMapper).writeValueAsString(voucherEntitlement);
    }

    @Test
    void shouldThrowExceptionWhenFailingToConvertVoucherEntitlementToString() throws JsonProcessingException {
        JsonProcessingException jsonException = new JsonParseException(null, "Unable to parse json");
        given(objectMapper.writeValueAsString(any())).willThrow(jsonException);
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().build();

        RuntimeException thrown = catchThrowableOfType(() -> converter.convertToDatabaseColumn(voucherEntitlement), RuntimeException.class);

        assertThat(thrown.getMessage()).isEqualTo("Unable to convert voucher entitlement " + voucherEntitlement.toString() +" into a json string");
        assertThat(thrown.getCause()).isEqualTo(jsonException);
    }

    @Test
    void shouldConvertJsonStringToVoucherEntitlement() throws IOException {
        String voucherEntitlementJson = "{pregnancyVoucher: 1}";
        VoucherEntitlement voucherEntitlement = VoucherEntitlement.builder().build();
        given(objectMapper.readValue(anyString(), eq(VoucherEntitlement.class))).willReturn(voucherEntitlement);

        VoucherEntitlement result = converter.convertToEntityAttribute(voucherEntitlementJson);

        assertThat(result).isEqualTo(voucherEntitlement);
        verify(objectMapper).readValue(voucherEntitlementJson, VoucherEntitlement.class);
    }

    @Test
    void shouldThrowExceptionWhenFailingToConvertStringToVoucherEntitlement() throws IOException {
        JsonProcessingException jsonException = new JsonParseException(null, "Unable to parse json");
        given(objectMapper.readValue(anyString(), eq(VoucherEntitlement.class))).willThrow(jsonException);
        String voucherEntitlementJson = "{pregnancyVoucher: 1}";

        RuntimeException thrown = catchThrowableOfType(() -> converter.convertToEntityAttribute(voucherEntitlementJson), RuntimeException.class);

        assertThat(thrown.getMessage()).isEqualTo("Unable to convert json string " + voucherEntitlementJson + " into a voucher entitlement object");
        assertThat(thrown.getCause()).isEqualTo(jsonException);
    }
}
