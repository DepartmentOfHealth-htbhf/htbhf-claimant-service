package uk.gov.dhsc.htbhf.claimant.exception;

import java.util.List;
import java.util.UUID;

/**
 * Exception thrown when multiple (live) claims are found for the same NINO.
 */
public class MultipleClaimsExistWithSameNinoException extends RuntimeException {

    public MultipleClaimsExistWithSameNinoException(List<UUID> claimIds) {
        super("Multiple live claims exist for the same NINO! Claim ids: " + claimIds);
    }
}
