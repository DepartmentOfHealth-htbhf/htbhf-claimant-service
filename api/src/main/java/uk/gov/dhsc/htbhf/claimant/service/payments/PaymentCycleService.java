package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;

import java.time.LocalDate;

@Service
public class PaymentCycleService {

    private final PaymentCycleRepository paymentCycleRepository;
    private final Integer cycleDurationInDays;

    public PaymentCycleService(PaymentCycleRepository paymentCycleRepository,
                               @Value("${payment-cycle.cycle-duration-in-days}") Integer cycleDurationInDays) {
        this.paymentCycleRepository = paymentCycleRepository;
        this.cycleDurationInDays = cycleDurationInDays;
    }

    /**
     * Creates a new payment cycle for the given claim. The cycle start date is set to the claim status
     * date, and the cycle end date is set to the start date, plus the payment cycle duration time.
     * @param claim claim to create a payment cycle for
     */
    public void createNewPaymentCycle(Claim claim) {
        LocalDate cycleStartDate = claim.getClaimStatusTimestamp().toLocalDate();
        PaymentCycle paymentCycle = PaymentCycle.builder()
                .claim(claim)
                .cycleStartDate(cycleStartDate)
                .cycleEndDate(cycleStartDate.plusDays(cycleDurationInDays))
                .build();
        paymentCycleRepository.save(paymentCycle);
    }
}
