package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;

@Component
public class VoucherEntitlementToDTOConverter {

    public VoucherEntitlementDTO convert(VoucherEntitlement from) {
        Assert.notNull(from, "VoucherEntitlement must not be null");
        return VoucherEntitlementDTO.builder()
                .vouchersForChildrenUnderOne(from.getVouchersForChildrenUnderOne())
                .vouchersForChildrenBetweenOneAndFour(from.getVouchersForChildrenBetweenOneAndFour())
                .vouchersForPregnancy(from.getVouchersForPregnancy())
                .totalVoucherEntitlement(from.getTotalVoucherEntitlement())
                .voucherValueInPence(from.getVoucherValueInPence())
                .totalVoucherValueInPence(from.getTotalVoucherValueInPence())
                .build();
    }
}
