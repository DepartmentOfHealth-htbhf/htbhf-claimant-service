package uk.gov.dhsc.htbhf.claimant.model;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.dwp.model.v2.*;

/**
 * Encapsulation of the extra verification outcome information that was returned from DWP.
 */
@Data
@Builder
@ApiModel(description = "The result of verifying the claimant's details")
public class VerificationResult {

    private final IdentityOutcome identityOutcome;

    private final EligibilityOutcome eligibilityOutcome;

    private final DeathVerificationFlag deathVerificationFlag;

    private final VerificationOutcome mobilePhoneMatch;

    private final VerificationOutcome emailAddressMatch;

    private final VerificationOutcome addressLine1Match;

    private final VerificationOutcome postcodeMatch;

    private final VerificationOutcome pregnantChildDOBMatch;

    private final QualifyingBenefits qualifyingBenefits;

}
