package uk.gov.dhsc.htbhf.claimant.repository;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;

import java.util.UUID;

/**
 * Clients should not refer to this interface directly - use ClaimRepository instead.
 */
public interface ClaimLazyLoader {

    /**
     * Creates a reference to a Claim object without loading that Claim from the database.
     * @param id the id of the Claim to create a reference for.
     * @return An uninitialised proxy for a Claim object.
     */
    Claim getLazyLoadingClaim(UUID id);
}

