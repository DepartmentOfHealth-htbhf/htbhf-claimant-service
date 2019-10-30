package uk.gov.dhsc.htbhf.claimant.testsupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;

@ExtendWith(MockitoExtension.class)
class EntityAgeAcceleratorTest {


    @Test
    void shouldFastForwardPaymentCycle() {

        final LocalDateTime originalCardBalanceTimestamp = LocalDateTime.now();
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .cardBalanceTimestamp(originalCardBalanceTimestamp)
                .build();
        final LocalDate originalStartDate = paymentCycle.getCycleStartDate();
        final List<LocalDate> originalChildrenDob = paymentCycle.getChildrenDob();
        final LocalDateTime originalClaimStatusTime = paymentCycle.getClaim().getClaimStatusTimestamp();
        final LocalDate originalEntitlementDate = paymentCycle.getVoucherEntitlement().getVoucherEntitlements().get(0).getEntitlementDate();

        EntityAgeAccelerator.ageObject(paymentCycle, 28);

        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(originalStartDate.minusDays(28));
        assertThat(paymentCycle.getCardBalanceTimestamp()).isEqualTo(originalCardBalanceTimestamp.minusDays(28));
        assertThat(paymentCycle.getChildrenDob().get(0)).isEqualTo(originalChildrenDob.get(0).minusDays(28));
        assertThat(paymentCycle.getChildrenDob().get(1)).isEqualTo(originalChildrenDob.get(1).minusDays(28));
        assertThat(paymentCycle.getVoucherEntitlement().getVoucherEntitlements().get(0).getEntitlementDate()).isEqualTo(originalEntitlementDate.minusDays(28));
        assertThat(paymentCycle.getClaim().getClaimStatusTimestamp()).isEqualTo(originalClaimStatusTime);
    }

}