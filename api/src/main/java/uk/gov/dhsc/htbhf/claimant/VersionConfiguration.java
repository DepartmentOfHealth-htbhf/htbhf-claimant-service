package uk.gov.dhsc.htbhf.claimant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;
import uk.gov.dhsc.htbhf.claimant.service.v2.EligibilityAndEntitlementServiceV2;
import uk.gov.dhsc.htbhf.claimant.service.v3.EligibilityAndEntitlementServiceV3;

/**
 * Configuration class for setting up the beans to support both v2 and v3 versions of the {@link uk.gov.dhsc.htbhf.claimant.controller.v2.ClaimController}.
 * Where there are multiple beans of the same type, v2 is currently made the Primary bean that we do not have to change any existing code
 * for the dependency injection to work correctly for that version.
 */
@Configuration
public class VersionConfiguration {

    @Bean
    public ClaimService v2ClaimService(ClaimRepository claimRepository,
                                       EligibilityAndEntitlementServiceV2 eligibilityAndEntitlementServiceV2,
                                       EventAuditor eventAuditor,
                                       ClaimMessageSender claimMessageSender) {
        return new ClaimService(claimRepository, eligibilityAndEntitlementServiceV2, eventAuditor, claimMessageSender);
    }

    @Primary
    @Bean
    public ClaimService v3ClaimService(ClaimRepository claimRepository,
                                       EligibilityAndEntitlementServiceV3 eligibilityAndEntitlementServiceV3,
                                       EventAuditor eventAuditor,
                                       ClaimMessageSender claimMessageSender) {
        return new ClaimService(claimRepository, eligibilityAndEntitlementServiceV3, eventAuditor, claimMessageSender);
    }
}
