package uk.gov.dhsc.htbhf.claimant.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

import java.io.IOException;
import javax.persistence.AttributeConverter;

/**
 * Converts a {@link VoucherEntitlement} to json and visa versa.
 * Used to read/write values in the database.
 */
@Component
@AllArgsConstructor
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class PaymentCycleVoucherEntitlementConverter implements AttributeConverter<PaymentCycleVoucherEntitlement, String> {

    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(PaymentCycleVoucherEntitlement voucherEntitlement) {
        try {
            return objectMapper.writeValueAsString(voucherEntitlement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Unable to convert voucher entitlement %s into a json string", voucherEntitlement.toString()), e);
        }
    }

    @Override
    public PaymentCycleVoucherEntitlement convertToEntityAttribute(String voucherEntitlement) {
        try {
            return objectMapper.readValue(voucherEntitlement, PaymentCycleVoucherEntitlement.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to convert json string %s into a voucher entitlement object", voucherEntitlement), e);
        }
    }
}
