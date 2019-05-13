package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;
import java.util.UUID;
import javax.transaction.Transactional;

@Component
@AllArgsConstructor
@Slf4j
public class CreateNewPaymentCycleJob {

    @Transactional
    public PaymentCycle createNewPaymentCycle(UUID claimId, LocalDate cycleStartDate) {
        // TODO: MGS: Implement this method
        return null;
    }
}
