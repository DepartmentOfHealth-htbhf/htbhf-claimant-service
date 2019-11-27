package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.dwp.model.v2.DeathVerificationFlag;
import uk.gov.dhsc.htbhf.dwp.model.v2.QualifyingBenefits;
import uk.gov.dhsc.htbhf.dwp.model.v2.VerificationOutcome;

/**
 * Encapsulation of the extra verification outcome information that was returned from DWP.
 */
@Data
@Builder
public class VerificationResult {

    private final DeathVerificationFlag deathVerificationFlag;

    private final VerificationOutcome mobilePhoneMatch;

    private final VerificationOutcome emailAddressMatch;

    private final VerificationOutcome addressLine1Match;

    private final VerificationOutcome postcodeMatch;

    private final VerificationOutcome pregnantChildDOBMatch;

    private final QualifyingBenefits qualifyingBenefits;

}
