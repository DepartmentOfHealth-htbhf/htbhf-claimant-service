package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.message.payload.DetermineEntitlementMessagePayload;

import java.util.UUID;

public class DetermineEntitlementMessagePayloadTestDataFactory {

    public static DetermineEntitlementMessagePayload aValidDetermineEntitlementMessagePayload() {
        return defaultBuilder().build();
    }

    private static DetermineEntitlementMessagePayload.DetermineEntitlementMessagePayloadBuilder defaultBuilder() {
        return DetermineEntitlementMessagePayload.builder()
                .claimId(UUID.randomUUID())
                .currentPaymentCycleId(UUID.randomUUID())
                .previousPaymentCycleId(UUID.randomUUID());
    }
}
