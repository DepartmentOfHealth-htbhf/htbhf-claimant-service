package uk.gov.dhsc.htbhf.claimant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.DuplicateClaimChecker;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementDecisionFactory;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;
import uk.gov.dhsc.htbhf.claimant.service.v2.EligibilityAndEntitlementServiceV2;
import uk.gov.dhsc.htbhf.claimant.service.v3.EligibilityAndEntitlementServiceV3;
import uk.gov.dhsc.htbhf.claimant.service.v3.EligibilityClientV3;

/**
 * Configuration class for setting up the beans to support both v2 and v3 versions of the {@link uk.gov.dhsc.htbhf.claimant.controller.v2.ClaimController}.
 * Where there are multiple beans of the same type, v2 is currently made the Primary bean that we do not have to change any existing code
 * for the dependency injection to work correctly for that version.
 */
@Configuration
public class VersionConfiguration {

    @Primary
    @Bean
    public ClaimService v2ClaimService(ClaimRepository claimRepository,
                                       EligibilityAndEntitlementServiceV2 eligibilityAndEntitlementServiceV2,
                                       EventAuditor eventAuditor,
                                       ClaimMessageSender claimMessageSender) {
        return new ClaimService(claimRepository, eligibilityAndEntitlementServiceV2, eventAuditor, claimMessageSender);
    }

    @Bean
    public EligibilityAndEntitlementServiceV3 eligibilityAndEntitlementServiceV3(EligibilityClientV3 client,
                                                                                 DuplicateClaimChecker duplicateClaimChecker,
                                                                                 ClaimRepository claimRepository,
                                                                                 PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator,
                                                                                 EligibilityAndEntitlementDecisionFactory factory) {
        return new EligibilityAndEntitlementServiceV3(client, duplicateClaimChecker, claimRepository, paymentCycleEntitlementCalculator, factory);
    }

    @Bean
    public ClaimService v3ClaimService(ClaimRepository claimRepository,
                                       EligibilityAndEntitlementServiceV3 eligibilityAndEntitlementServiceV3,
                                       EventAuditor eventAuditor,
                                       ClaimMessageSender claimMessageSender) {
        return new ClaimService(claimRepository, eligibilityAndEntitlementServiceV3, eventAuditor, claimMessageSender);
    }
}
