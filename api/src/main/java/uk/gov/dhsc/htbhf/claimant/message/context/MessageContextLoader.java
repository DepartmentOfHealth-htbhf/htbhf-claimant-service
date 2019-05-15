package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessingException;
import uk.gov.dhsc.htbhf.claimant.message.payload.DetermineEntitlementMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;

import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class MessageContextLoader {

    private ClaimRepository claimRepository;

    private PaymentCycleRepository paymentCycleRepository;

    /**
     * Method used to inflate the contents of the objects identified by ids in the DETERMINE_ENTITLEMENT message payload.
     *
     * @param payload The payload containing the object ids
     * @return A wrapper object with the inflated objects
     */
    public DetermineEntitlementMessageContext loadContext(DetermineEntitlementMessagePayload payload) {

        PaymentCycle currentPaymentCycle = getAndCheckPaymentCycle(payload.getCurrentPaymentCycleId(), "current payment cycle");
        PaymentCycle previousPaymentCycle = getAndCheckPaymentCycle(payload.getPreviousPaymentCycleId(), "previous payment cycle");
        Claim claim = getAndCheckClaim(payload.getClaimId());

        return DetermineEntitlementMessageContext.builder()
                .currentPaymentCycle(currentPaymentCycle)
                .previousPaymentCycle(previousPaymentCycle)
                .claim(claim)
                .build();
    }

    /**
     * Method used to inflate the contents of the objects identified by ids in the MAKE_PAYMENT message payload.
     *
     * @param payload The payload containing the object ids
     * @return A wrapper object with the inflated objects
     */
    public MakePaymentMessageContext loadContext(MakePaymentMessagePayload payload) {
        PaymentCycle paymentCycle = getAndCheckPaymentCycle(payload.getPaymentCycleId(), "payment cycle");
        Claim claim = getAndCheckClaim(payload.getClaimId());

        return MakePaymentMessageContext.builder()
                .cardAccountId(payload.getCardAccountId())
                .claim(claim)
                .paymentCycle(paymentCycle)
                .build();
    }

    private Claim getAndCheckClaim(UUID claimId) {
        Optional<Claim> claim = claimRepository.findById(claimId);
        if (claim.isEmpty()) {
            logAndThrowException("claim", claimId);
        }
        return claim.get();
    }

    private PaymentCycle getAndCheckPaymentCycle(UUID paymentCycleId, String cycleName) {
        Optional<PaymentCycle> paymentCycle = paymentCycleRepository.findById(paymentCycleId);
        if (paymentCycle.isEmpty()) {
            logAndThrowException(cycleName, paymentCycleId);
        }
        return paymentCycle.get();
    }

    private void logAndThrowException(String fieldName, UUID uuid) {
        String errorMessage = String.format("Unable to process message, unable to load %s using id: %s", fieldName, uuid);
        log.error(errorMessage);
        throw new MessageProcessingException(errorMessage);
    }

}
