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

/**
 * Component that inflates a {@link MessagePayload} into the appropriate MessageContext by loading the objects identified by id in the payload.
 */
@Component
@AllArgsConstructor
@Slf4j
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class MessageContextLoader {

    private ClaimRepository claimRepository;

    private PaymentCycleRepository paymentCycleRepository;

    private PayloadMapper payloadMapper;

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

    public MakePaymentMessageContext loadMakePaymentContext(Message message) {

        MakePaymentMessagePayload payload = payloadMapper.getPayload(message, MakePaymentMessagePayload.class);

        PaymentCycle paymentCycle = getAndCheckPaymentCycle(payload.getPaymentCycleId(), "payment cycle");
        Claim claim = getAndCheckClaim(payload.getClaimId());

        return MakePaymentMessageContext.builder()
                .cardAccountId(payload.getCardAccountId())
                .claim(claim)
                .paymentCycle(paymentCycle)
                .paymentRestarted(payload.isPaymentRestarted())
                .build();
    }

    public RequestNewCardMessageContext loadRequestNewCardContext(Message message) {
        RequestNewCardMessagePayload payload = payloadMapper.getPayload(message, RequestNewCardMessagePayload.class);

        Claim claim = getAndCheckClaim(payload.getClaimId());
        return RequestNewCardMessageContext.builder()
                .claim(claim)
                .eligibilityAndEntitlementDecision(payload.getEligibilityAndEntitlementDecision())
                .build();
    }

    public CompleteNewCardMessageContext loadCompleteNewCardContext(Message message) {
        CompleteNewCardMessagePayload payload = payloadMapper.getPayload(message, CompleteNewCardMessagePayload.class);

        Claim claim = getAndCheckClaim(payload.getClaimId());
        return CompleteNewCardMessageContext.builder()
                .claim(claim)
                .cardAccountId(payload.getCardAccountId())
                .eligibilityAndEntitlementDecision(payload.getEligibilityAndEntitlementDecision())
                .build();
    }

    public EmailMessageContext loadEmailMessageContext(Message message) {
        EmailMessagePayload payload = payloadMapper.getPayload(message, EmailMessagePayload.class);

        Claim claim = getAndCheckClaim(payload.getClaimId());
        return EmailMessageContext.builder()
                .claim(claim)
                .emailPersonalisation(payload.getEmailPersonalisation())
                .emailType(payload.getEmailType())
                .build();
    }

    public LetterMessageContext loadLetterMessageContext(Message message) {
        LetterMessagePayload payload = payloadMapper.getPayload(message, LetterMessagePayload.class);

        Claim claim = getAndCheckClaim(payload.getClaimId());
        return LetterMessageContext.builder()
                .claim(claim)
                .personalisation(payload.getPersonalisation())
                .letterType(payload.getLetterType())
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
                .identityAndEligibilityResponse(payload.getIdentityAndEligibilityResponse())
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
                .paymentForBackdatedVouchers(payload.getPaymentForBackdatedVouchers())
                .identityAndEligibilityResponse(payload.getIdentityAndEligibilityResponse())
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
