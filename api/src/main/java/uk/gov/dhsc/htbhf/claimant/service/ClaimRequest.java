package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.util.Map;

/**
 * The content of a createOrUpdateClaim request.
 */
@Data
@Builder
public class ClaimRequest {
    private Claimant claimant;
    private Map<String, Object> deviceFingerprint;
    private String webUIVersion;
}
