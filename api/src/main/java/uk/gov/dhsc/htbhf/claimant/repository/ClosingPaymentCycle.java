package uk.gov.dhsc.htbhf.claimant.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Identifies a payment cycle due to expire on cycleEndDate.
 */
public interface ClosingPaymentCycle {

    String getClaimIdString();

    String getCycleIdString();

    Timestamp getCycleEndDateTimestamp();

    default UUID getClaimId() {
        return UUID.fromString(getClaimIdString());
    }

    default UUID getCycleId() {
        return UUID.fromString(getCycleIdString());
    }

    default LocalDate getCycleEndDate() {
        return getCycleEndDateTimestamp().toLocalDateTime().toLocalDate();
    }
}
