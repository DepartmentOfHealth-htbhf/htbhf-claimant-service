package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;

import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResultTestDataFactory {

    public static PaymentResult aValidPaymentResult() {
        return PaymentResult.builder()
                .paymentTimestamp(LocalDateTime.now())
                .requestReference(UUID.randomUUID().toString())
                .responseReference(UUID.randomUUID().toString())
                .build();
    }
}
