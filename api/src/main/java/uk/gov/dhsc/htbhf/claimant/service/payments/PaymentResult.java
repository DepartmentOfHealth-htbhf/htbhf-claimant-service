package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PaymentResult {
    private String requestReference;
    private String responseReference;
    private LocalDateTime paymentTimestamp;
}
