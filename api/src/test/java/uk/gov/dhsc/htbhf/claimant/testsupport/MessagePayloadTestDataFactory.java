package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.message.payload.*;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aValidDecisionBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

public class MessagePayloadTestDataFactory {

    private static final UUID CLAIM_ID = UUID.fromString("bc78da28-5bea-45fd-95ca-8bd82979c584");

    public static final String NEW_CARD_PAYLOAD_JSON = "{ \"claimId\":\"" + CLAIM_ID.toString() + "\"}";

    public static MakePaymentMessagePayload aMakePaymentPayload(UUID claimId, UUID paymentCycleId, PaymentType paymentType) {
        return MakePaymentMessagePayload.builder()
                .claimId(claimId)
                .paymentCycleId(paymentCycleId)
                .cardAccountId(CARD_ACCOUNT_ID)
                .paymentType(paymentType)
                .build();
    }

    public static RequestPaymentMessagePayload aRequestPaymentPayload(UUID claimId, UUID paymentCycleId, PaymentType paymentType) {
        return RequestPaymentMessagePayload.builder()
                .claimId(claimId)
                .paymentCycleId(paymentCycleId)
                .paymentType(paymentType)
                .build();
    }

    public static CompletePaymentMessagePayload aCompletePaymentPayload(UUID claimId,
                                                                        UUID paymentCycleId,
                                                                        PaymentCalculation paymentCalculation,
                                                                        PaymentResult paymentResult) {
        return CompletePaymentMessagePayload.builder()
                .claimId(claimId)
                .paymentCycleId(paymentCycleId)
                .paymentCalculation(paymentCalculation)
                .paymentResult(paymentResult)
                .build();
    }

    public static RequestNewCardMessagePayload aValidNewCardRequestMessagePayload() {
        return defaultRequestNewCardMessagePayloadBuilder().build();
    }

    public static RequestNewCardMessagePayload aRequestNewCardMessagePayload(UUID claimId,
                                                                             PaymentCycleVoucherEntitlement voucherEntitlement,
                                                                             List<LocalDate> datesOfBirth) {
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = aValidDecisionBuilder()
                .voucherEntitlement(voucherEntitlement)
                .build();
        return defaultRequestNewCardMessagePayloadBuilder()
                .claimId(claimId)
                .eligibilityAndEntitlementDecision(eligibilityAndEntitlementDecision)
                .build();
    }

    public static CompleteNewCardMessagePayload aCompleteNewCardMessagePayload(UUID claimId) {
        return CompleteNewCardMessagePayload.builder()
                .claimId(claimId)
                .cardAccountId(CARD_ACCOUNT_ID)
                .eligibilityAndEntitlementDecision(anEligibleDecision())
                .build();
    }

    private static RequestNewCardMessagePayload.RequestNewCardMessagePayloadBuilder defaultRequestNewCardMessagePayloadBuilder() {
        return RequestNewCardMessagePayload.builder()
                .claimId(CLAIM_ID)
                .eligibilityAndEntitlementDecision(anEligibleDecision());
    }

}
