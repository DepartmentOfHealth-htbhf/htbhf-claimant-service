package uk.gov.dhsc.htbhf.claimant.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.DetermineEntitlementMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;

import java.util.Optional;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;

@Component
@AllArgsConstructor
@Slf4j
public class DetermineEntitlementMessageContextLoader {

    private ClaimRepository claimRepository;

    private PaymentCycleRepository paymentCycleRepository;

    /**
     * Method used to inflate the contents of the objects identified by ids in the DETERMINE_ENTITLEMENT message payload.
     *
     * @param payload The payload containing the object ids
     * @return A wrapper object with the inflated objects
     */
    public DetermineEntitlementMessageContext loadContext(DetermineEntitlementMessagePayload payload) {

        PaymentCycle currentPaymentCycle = getAndCheckCurrentPaymentCycle(payload);
        PaymentCycle previousPaymentCycle = getAndCheckPreviousPaymentCycle(payload);
        Claim claim = getAndCheckClaim(payload);

        return DetermineEntitlementMessageContext.builder()
                .currentPaymentCycle(currentPaymentCycle)
                .previousPaymentCycle(previousPaymentCycle)
                .claim(claim)
                .build();
    }

    private Claim getAndCheckClaim(DetermineEntitlementMessagePayload payload) {
        UUID claimId = payload.getClaimId();
        Optional<Claim> claim = claimRepository.findById(claimId);
        if (claim.isEmpty()) {
            logAndThrowException("claim", claimId);
        }
        return claim.get();
    }

    private PaymentCycle getAndCheckCurrentPaymentCycle(DetermineEntitlementMessagePayload payload) {
        UUID currentPaymentCycleId = payload.getCurrentPaymentCycleId();
        return getAndCheckPaymentCycle(currentPaymentCycleId, "current payment cycle");
    }

    private PaymentCycle getAndCheckPreviousPaymentCycle(DetermineEntitlementMessagePayload payload) {
        UUID previousPaymentCycleId = payload.getPreviousPaymentCycleId();
        return getAndCheckPaymentCycle(previousPaymentCycleId, "previous payment cycle");
    }

    private PaymentCycle getAndCheckPaymentCycle(UUID paymentCycleId, String cycleName) {
        Optional<PaymentCycle> paymentCycle = paymentCycleRepository.findById(paymentCycleId);
        if (paymentCycle.isEmpty()) {
            logAndThrowException(cycleName, paymentCycleId);
        }
        return paymentCycle.get();
    }

    private void logAndThrowException(String fieldName, UUID uuid) {
        String errorMessage = String.format("Unable to process %s message, unable to load %s using id: %s", DETERMINE_ENTITLEMENT, fieldName, uuid);
        log.error(errorMessage);
        throw new MessageProcessingException(errorMessage);
    }

}
