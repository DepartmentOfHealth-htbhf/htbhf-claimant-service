package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;

@Service
@AllArgsConstructor
public class PaymentCycleService {
    private PaymentCycleRepository paymentCycleRepository;

    public void createNewPaymentCycle(Claim claim) {
        // TODO add cycle start/end date
        PaymentCycle paymentCycle = PaymentCycle.builder().claim(claim).build();
        paymentCycleRepository.save(paymentCycle);
    }
}
