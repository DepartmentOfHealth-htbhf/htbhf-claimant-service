package uk.gov.dhsc.htbhf.claimant.service.claim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

@ExtendWith(MockitoExtension.class)
class ClaimActivationServiceTest {

    @Mock
    ClaimRepository claimRepository;
    @Mock
    PaymentCycleService paymentCycleService;
    @Mock
    EventAuditor eventAuditor;
    @Mock
    ClaimMessageSender claimMessageSender;

    @InjectMocks
    ClaimActivationService claimActivationService;


    @Test
    void shouldUpdateClaimAndReportChangesAndCreatePaymentCycle() {
        LocalDateTime originalStatusTimestamp = LocalDateTime.now().minusDays(1);
        Claim claim = aValidClaimBuilder()
                .claimStatusTimestamp(originalStatusTimestamp)
                .cardAccountId(null)
                .build();
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);
        given(paymentCycleService.createAndSavePaymentCycleForEligibleClaim(any(), any(), any())).willReturn(paymentCycle);
        EligibilityAndEntitlementDecision decision = anEligibleDecision();

        PaymentCycle result = claimActivationService.updateClaimAndCreatePaymentCycle(claim, CARD_ACCOUNT_ID, decision);

        assertThat(result).isEqualTo(paymentCycle);
        assertThat(claim.getCardAccountId()).isEqualTo(CARD_ACCOUNT_ID);
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        verify(claimRepository).save(claim);
        verify(paymentCycleService).createAndSavePaymentCycleForEligibleClaim(claim, originalStatusTimestamp.toLocalDate(), decision);
        verify(eventAuditor).auditNewCard(claim.getId(), CARD_ACCOUNT_ID);
        verify(claimMessageSender).sendReportClaimMessage(claim, decision.getIdentityAndEligibilityResponse(), ClaimAction.UPDATED_FROM_NEW_TO_ACTIVE);
    }
}