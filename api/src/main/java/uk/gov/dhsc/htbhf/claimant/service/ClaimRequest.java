package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;

import java.util.Map;

/**
 * The content of a createClaim request.
 */
@Data
@Builder
public class ClaimRequest {
    private Claimant claimant;
    private Map<String, Object> deviceFingerprint;
    private String webUIVersion;
    private EligibilityOverride eligibilityOverride;
}
