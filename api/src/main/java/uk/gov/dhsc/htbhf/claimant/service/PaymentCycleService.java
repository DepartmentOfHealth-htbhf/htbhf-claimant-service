package uk.gov.dhsc.htbhf.claimant.service;

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
        PaymentCycle paymentCycle = PaymentCycle.builder().claim(claim).build();
        paymentCycleRepository.save(paymentCycle);
    }
}
