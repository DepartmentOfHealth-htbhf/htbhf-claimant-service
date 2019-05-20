package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;

import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

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
     * Creates a new payment cycle for the given claim and cycle start date, saving it in the database.
     * The cycle end date is set to the start date plus the payment cycle duration time.
     *
     * @param claim          claim to create a payment cycle for
     * @param cycleStartDate the start date of the new payment cycle
     * @return the new payment cycle.
     */
    public PaymentCycle createAndSavePaymentCycle(Claim claim, LocalDate cycleStartDate) {
        PaymentCycle paymentCycle = PaymentCycle.builder()
                .claim(claim)
                .cycleStartDate(cycleStartDate)
                .cycleEndDate(cycleStartDate.plusDays(cycleDurationInDays))
                .build();
        paymentCycleRepository.save(paymentCycle);
        return paymentCycle;
    }

    /**
     * Creates a new payment cycle, saving it in the database.
     * The cycle end date is set to the start date plus the payment cycle duration time.
     * EligibilityStatus is set to ELIGIBLE.
     *
     * @param claim              claim to create a payment cycle for
     * @param cycleStartDate     the start date of the new payment cycle
     * @param voucherEntitlement the vouchers the claimant is entitled to during this cycle
     * @return the new payment cycle.
     */
    public PaymentCycle createAndSavePaymentCycleForEligibleClaim(Claim claim, LocalDate cycleStartDate, PaymentCycleVoucherEntitlement voucherEntitlement) {
        PaymentCycle paymentCycle = PaymentCycle.builder()
                .claim(claim)
                .cycleStartDate(cycleStartDate)
                .cycleEndDate(cycleStartDate.plusDays(cycleDurationInDays))
                .eligibilityStatus(ELIGIBLE)
                .build();
        paymentCycle.applyVoucherEntitlement(voucherEntitlement);
        paymentCycleRepository.save(paymentCycle);
        return paymentCycle;
    }

    /**
     * Update the PaymentCycle with the eligibility status and voucher entitlement and save to the db.
     *
     * @param paymentCycle      The payment cycle
     * @param eligibilityStatus The eligibility status
     * @param entitlement       The entitlement
     */
    public void updateAndSavePaymentCycle(PaymentCycle paymentCycle, EligibilityStatus eligibilityStatus, PaymentCycleVoucherEntitlement entitlement) {
        paymentCycle.setEligibilityStatus(eligibilityStatus);
        paymentCycle.applyVoucherEntitlement(entitlement);
        paymentCycleRepository.save(paymentCycle);
    }
}
