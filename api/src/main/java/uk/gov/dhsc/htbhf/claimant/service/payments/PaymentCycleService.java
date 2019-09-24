package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.NEW;
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
                .paymentCycleStatus(NEW)
                .cycleStartDate(cycleStartDate)
                .cycleEndDate(cycleStartDate.plusDays(cycleDurationInDays - 1))
                .build();
        paymentCycleRepository.save(paymentCycle);
        return paymentCycle;
    }

    /**
     * Creates a new payment cycle, saving it in the database.
     * The cycle end date is set to the start date plus the payment cycle duration time.
     * The expected due date is set only if the voucher entitlement contains pregnancy vouchers.
     * EligibilityStatus is set to ELIGIBLE.
     *
     * @param claim                  claim to create a payment cycle for
     * @param cycleStartDate         the start date of the new payment cycle
     * @param voucherEntitlement     the vouchers the claimant is entitled to during this cycle
     * @param datesOfBirthOfChildren the dates of birth of children present during this cycle
     * @return the new payment cycle.
     */
    public PaymentCycle createAndSavePaymentCycleForEligibleClaim(Claim claim,
                                                                  LocalDate cycleStartDate,
                                                                  PaymentCycleVoucherEntitlement voucherEntitlement,
                                                                  List<LocalDate> datesOfBirthOfChildren) {
        PaymentCycle paymentCycle = PaymentCycle.builder()
                .claim(claim)
                .paymentCycleStatus(NEW)
                .cycleStartDate(cycleStartDate)
                .cycleEndDate(cycleStartDate.plusDays(cycleDurationInDays - 1))
                .eligibilityStatus(ELIGIBLE)
                .childrenDob(datesOfBirthOfChildren)
                .expectedDeliveryDate(getExpectedDeliveryDateIfRelevant(claim, voucherEntitlement))
                .build();

        paymentCycle.applyVoucherEntitlement(voucherEntitlement);
        paymentCycleRepository.save(paymentCycle);
        return paymentCycle;
    }

    /**
     * Save the PaymentCycle to the database.
     *
     * @param paymentCycle The {@link PaymentCycle} to save
     */
    public void savePaymentCycle(PaymentCycle paymentCycle) {
        paymentCycleRepository.save(paymentCycle);
    }

    public LocalDate getExpectedDeliveryDateIfRelevant(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement) {
        return voucherEntitlement.getVouchersForPregnancy() > 0 ? claim.getClaimant().getExpectedDeliveryDate() : null;
    }

    /**
     * Update and saves the payment cycle with the given calculation and card balance.
     * @param paymentCycle payment cycle to update
     * @param paymentCycleStatus payment cycle status
     * @param cardBalanceInPence card balance in pence
     */
    public void updatePaymentCycle(PaymentCycle paymentCycle, PaymentCycleStatus paymentCycleStatus, int cardBalanceInPence) {
        paymentCycle.setCardBalanceInPence(cardBalanceInPence);
        paymentCycle.setCardBalanceTimestamp(LocalDateTime.now());
        updatePaymentCycle(paymentCycle, paymentCycleStatus);
    }

    /**
     * Update and saves the payment cycle with the decision.
     * @param paymentCycle payment cycle to update
     * @param decision decision to update the payment cycle with
     */
    public void updatePaymentCycle(PaymentCycle paymentCycle, EligibilityAndEntitlementDecision decision) {
        paymentCycle.setEligibilityStatus(decision.getEligibilityStatus());
        paymentCycle.setChildrenDob(decision.getDateOfBirthOfChildren());
        paymentCycle.applyVoucherEntitlement(decision.getVoucherEntitlement());
        PaymentCycleStatus paymentCycleStatus = PaymentCycleStatus.getStatusForEligibilityDecision(decision.getEligibilityStatus());
        paymentCycle.setPaymentCycleStatus(paymentCycleStatus);
        LocalDate expectedDeliveryDate = getExpectedDeliveryDateIfRelevant(paymentCycle.getClaim(), decision.getVoucherEntitlement());
        paymentCycle.setExpectedDeliveryDate(expectedDeliveryDate);
        paymentCycleRepository.save(paymentCycle);
    }

    /**
     * Update and saves the payment cycle with the given calculation.
     * @param paymentCycle payment cycle to update
     * @param paymentCycleStatus payment cycle status
     */
    public void updatePaymentCycle(PaymentCycle paymentCycle, PaymentCycleStatus paymentCycleStatus) {
        paymentCycle.setPaymentCycleStatus(paymentCycleStatus);
        paymentCycleRepository.save(paymentCycle);
    }
}
