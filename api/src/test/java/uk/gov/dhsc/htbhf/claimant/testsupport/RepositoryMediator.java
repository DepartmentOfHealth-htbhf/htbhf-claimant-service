package uk.gov.dhsc.htbhf.claimant.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;

/**
 * Mediates between test case and the repositories to create & persist entities, and ensure that loaded entities are fully initialised.
 */
@Component
public class RepositoryMediator {

    @Autowired
    private ClaimRepository claimRepository;
    @Autowired
    private PaymentCycleRepository paymentCycleRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private MessageRepository messageRepository;

    /**
     * Asserts that there is a current PaymentCycle, and initialises the collections of the returned PaymentCycle.
     *
     * @param claim the claim to get the cycle for.
     * @return the current PaymentCycle.
     */
    @Transactional
    public PaymentCycle getCurrentPaymentCycleForClaim(Claim claim) {
        Optional<PaymentCycle> cycleOptional = paymentCycleRepository.findCurrentCycleForClaim(claim);
        assertThat(cycleOptional).isPresent();
        PaymentCycle paymentCycle = cycleOptional.get();
        // ensure that everything has been loaded
        paymentCycle.getPayments().size();
        paymentCycle.getClaim().toString();
        return paymentCycle;
    }

    @Transactional
    public Claim getClaimForNino(String nino) {
        List<UUID> claimIds = claimRepository.findLiveClaimsWithNino(nino);
        assertThat(claimIds).isNotEmpty();
        Optional<Claim> optional = claimRepository.findById(claimIds.get(0));
        assertThat(optional.isPresent()).isTrue();
        Claim claim = optional.get();
        // ensure that everything has been loaded
        claim.getClaimant().toString();
        claim.getClaimant().getAddress().toString();
        return claim;
    }

    /**
     * Invokes deleteAll on all repositories we have access to.
     */
    public void deleteAllEntities() {
        messageRepository.deleteAll();
        paymentRepository.deleteAll();
        paymentCycleRepository.deleteAll();
        claimRepository.deleteAll();
    }

    /**
     * Ensures the given claim is persistent, then creates and persists a PaymentCycle for the given start date.
     * The payment cycle is created in a completed state (the payment having been made) based on the dates of birth,
     * the expected due date of the claimant and an assumption that they are eligible.
     *
     * @param claim                 the claim to create a paymentCycle for.
     * @param cycleStartDate        the start date of the cycle.
     * @param childrensDatesOfBirth dates of birth of children.
     * @return the persistent PaymentCycle.
     */
    public PaymentCycle createAndSavePaymentCycle(Claim claim, LocalDate cycleStartDate, List<LocalDate> childrensDatesOfBirth) {
        claimRepository.save(claim);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(
                cycleStartDate, childrensDatesOfBirth, claim.getClaimant().getExpectedDeliveryDate());
        PaymentCycle completedPaymentCycle = PaymentCycleTestDataFactory.aValidPaymentCycleBuilder()
                .claim(claim)
                .childrenDob(childrensDatesOfBirth)
                .cycleStartDate(cycleStartDate)
                .cycleEndDate(cycleStartDate.plusDays(27))
                .paymentCycleStatus(PaymentCycleStatus.FULL_PAYMENT_MADE)
                .voucherEntitlement(voucherEntitlement)
                .expectedDeliveryDate(getExpectedDeliveryDateIfRelevant(claim, voucherEntitlement))
                .build();
        paymentCycleRepository.save(completedPaymentCycle);
        return completedPaymentCycle;
    }

    //Copied from PaymentCycleService - don't want to inject it just for this method.
    private LocalDate getExpectedDeliveryDateIfRelevant(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement) {
        return (voucherEntitlement != null && voucherEntitlement.getVouchersForPregnancy() > 0) ? claim.getClaimant().getExpectedDeliveryDate() : null;
    }

    /**
     * Loads the claim from the database with the given claim id.
     *
     * @param claimId The id of the claim
     * @return The claim from the database
     */
    public Claim loadClaim(UUID claimId) {
        return claimRepository.findClaim(claimId);
    }

    /**
     * Saves the given claim.
     *
     * @param claim The claim to be saved
     */
    public void saveClaim(Claim claim) {
        claimRepository.save(claim);
    }

    /**
     * Fast-forwards the database by a given number of days. Subtracts the number of days from every temporal field on every entity in the database.
     * This updates all payments, paymentCycles, claims and claimants in the database - it could be problematic if run on a large db.
     * @param numberOfDays the number of days to fast-forward by.
     */
    @Transactional
    public void ageDatabaseEntities(int numberOfDays) {
        paymentRepository.findAll().forEach(payment -> EntityAgeAccelerator.ageObject(payment, numberOfDays));
        claimRepository.findAll().forEach(claim -> {
            EntityAgeAccelerator.ageObject(claim, numberOfDays);
            EntityAgeAccelerator.ageObject(claim.getClaimant(), numberOfDays);
        });
        paymentCycleRepository.findAll().forEach(paymentCycle -> EntityAgeAccelerator.ageObject(paymentCycle, numberOfDays));
        messageRepository.findAll().forEach(message -> EntityAgeAccelerator.ageObject(message, numberOfDays));
    }

    /**
     * Ensures that the messageTimestamp on all messages is in the past.
     */
    @Transactional
    public void makeAllMessagesProcessable() {
        messageRepository.findAll().forEach(message -> EntityAgeAccelerator.fastForward(message, 1));
    }
}
