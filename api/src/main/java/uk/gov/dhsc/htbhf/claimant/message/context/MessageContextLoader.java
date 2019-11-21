package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessingException;
import uk.gov.dhsc.htbhf.claimant.message.PayloadMapper;
import uk.gov.dhsc.htbhf.claimant.message.payload.*;
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

    private PayloadMapper payloadMapper;

    /**
     * Method used to inflate the contents of the objects identified by ids in the DETERMINE_ENTITLEMENT message payload.
     *
     * @param message The message containing the payload to inflate
     * @return A wrapper object with the inflated objects from the message
     */
    public DetermineEntitlementMessageContext loadDetermineEntitlementContext(Message message) {

        DetermineEntitlementMessagePayload payload = payloadMapper.getPayload(message, DetermineEntitlementMessagePayload.class);

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
     * @param message The message containing the payload to inflate
     * @return A wrapper object with the inflated objects
     */
    public MakePaymentMessageContext loadMakePaymentContext(Message message) {

        MakePaymentMessagePayload payload = payloadMapper.getPayload(message, MakePaymentMessagePayload.class);

        PaymentCycle paymentCycle = getAndCheckPaymentCycle(payload.getPaymentCycleId(), "payment cycle");
        Claim claim = getAndCheckClaim(payload.getClaimId());

        return MakePaymentMessageContext.builder()
                .cardAccountId(payload.getCardAccountId())
                .claim(claim)
                .paymentCycle(paymentCycle)
                .build();
    }

    /**
     * Method used to inflate the contents of a NEW_CARD message, which is currently just the claim.
     *
     * @param message The message to inflate.
     * @return A wrapper object with the inflated objects
     */
    public RequestNewCardMessageContext loadRequestNewCardContext(Message message) {
        RequestNewCardMessagePayload payload = payloadMapper.getPayload(message, RequestNewCardMessagePayload.class);

        Claim claim = getAndCheckClaim(payload.getClaimId());
        return RequestNewCardMessageContext.builder()
                .claim(claim)
                .eligibilityAndEntitlementDecision(payload.getEligibilityAndEntitlementDecision())
                .build();
    }

    public SaveNewCardMessageContext loadSaveNewCardContext(Message message) {
        SaveNewCardMessagePayload payload = payloadMapper.getPayload(message, SaveNewCardMessagePayload.class);

        Claim claim = getAndCheckClaim(payload.getClaimId());
        return SaveNewCardMessageContext.builder()
                .claim(claim)
                .cardAccountId(payload.getCardAccountId())
                .eligibilityAndEntitlementDecision(payload.getEligibilityAndEntitlementDecision())
                .build();
    }

    /**
     * Method used to inflate the contents of a SEND_EMAIL message.
     *
     * @param message The message to inflate.
     * @return A wrapper object with the inflated objects
     */
    public EmailMessageContext loadEmailMessageContext(Message message) {
        EmailMessagePayload payload = payloadMapper.getPayload(message, EmailMessagePayload.class);

        Claim claim = getAndCheckClaim(payload.getClaimId());
        String templateId = payload.getEmailType().getTemplateId();
        return EmailMessageContext.builder()
                .claim(claim)
                .templateId(templateId)
                .emailPersonalisation(payload.getEmailPersonalisation())
                .emailType(payload.getEmailType())
                .build();
    }

    public AdditionalPregnancyPaymentMessageContext loadAdditionalPregnancyPaymentMessageContext(Message message) {
        AdditionalPregnancyPaymentMessagePayload payload = payloadMapper.getPayload(message, AdditionalPregnancyPaymentMessagePayload.class);

        Claim claim = getAndCheckClaim(payload.getClaimId());
        Optional<PaymentCycle> paymentCycle = paymentCycleRepository.findCurrentCycleForClaim(claim);

        return AdditionalPregnancyPaymentMessageContext.builder()
                .claim(claim)
                .paymentCycle(paymentCycle)
                .build();
    }

    public ReportClaimMessageContext loadReportClaimMessageContext(Message message) {
        ReportClaimMessagePayload payload = payloadMapper.getPayload(message, ReportClaimMessagePayload.class);
        Claim claim = getAndCheckClaim(payload.getClaimId());

        return ReportClaimMessageContext.builder()
                .claim(claim)
                .claimAction(payload.getClaimAction())
                .datesOfBirthOfChildren(payload.getDatesOfBirthOfChildren())
                .timestamp(payload.getTimestamp())
                .updatedClaimantFields(payload.getUpdatedClaimantFields())
                .build();
    }

    public ReportPaymentMessageContext loadReportPaymentMessageContext(Message message) {
        ReportPaymentMessagePayload payload = payloadMapper.getPayload(message, ReportPaymentMessagePayload.class);
        Claim claim = getAndCheckClaim(payload.getClaimId());
        PaymentCycle paymentCycle = getAndCheckPaymentCycle(payload.getPaymentCycleId(), "payment cycle");

        return ReportPaymentMessageContext.builder()
                .claim(claim)
                .paymentCycle(paymentCycle)
                .paymentForPregnancy(payload.getPaymentForPregnancy())
                .paymentForChildrenUnderOne(payload.getPaymentForChildrenUnderOne())
                .paymentForChildrenBetweenOneAndFour(payload.getPaymentForChildrenBetweenOneAndFour())
                .datesOfBirthOfChildren(payload.getDatesOfBirthOfChildren())
                .paymentAction(payload.getPaymentAction())
                .timestamp(payload.getTimestamp())
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
