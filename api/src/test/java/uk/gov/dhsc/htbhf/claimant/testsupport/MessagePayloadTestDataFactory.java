package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.RequestNewCardMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aValidDecisionBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

public class MessagePayloadTestDataFactory {

    private static final UUID CLAIM_ID = UUID.fromString("bc78da28-5bea-45fd-95ca-8bd82979c584");

    public static final String NEW_CARD_PAYLOAD_JSON = "{ \"claimId\":\"" + CLAIM_ID.toString() + "\"}";

    public static MakePaymentMessagePayload aMakePaymentPayload(UUID claimId, UUID paymentCycleId) {
        return MakePaymentMessagePayload.builder()
                .claimId(claimId)
                .paymentCycleId(paymentCycleId)
                .cardAccountId(CARD_ACCOUNT_ID)
                .build();
    }

    public static RequestNewCardMessagePayload aValidNewCardRequestMessagePayload() {
        return defaultNewCardRequestMessagePayloadBuilder().build();
    }

    public static RequestNewCardMessagePayload aNewCardRequestMessagePayload(UUID claimId,
                                                                             PaymentCycleVoucherEntitlement voucherEntitlement,
                                                                             List<LocalDate> datesOfBirth) {
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = aValidDecisionBuilder()
                .voucherEntitlement(voucherEntitlement)
                .dateOfBirthOfChildren(datesOfBirth)
                .build();
        return defaultNewCardRequestMessagePayloadBuilder()
                .claimId(claimId)
                .eligibilityAndEntitlementDecision(eligibilityAndEntitlementDecision)
                .build();
    }

    private static RequestNewCardMessagePayload.RequestNewCardMessagePayloadBuilder defaultNewCardRequestMessagePayloadBuilder() {
        return RequestNewCardMessagePayload.builder()
                .claimId(CLAIM_ID)
                .eligibilityAndEntitlementDecision(anEligibleDecision());
    }

}
