package uk.gov.dhsc.htbhf.claimant.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Identifies a claim with a payment cycle due to expire on cycleEndDate.
 */
public interface ClosingPaymentCycle {

    String getClaimIdString();

    Timestamp getCycleEndDateTimestamp();

    default UUID getClaimId() {
        return UUID.fromString(getClaimIdString());
    }

    default LocalDate getCycleEndDate() {
        return getCycleEndDateTimestamp().toLocalDateTime().toLocalDate();
    }
}
