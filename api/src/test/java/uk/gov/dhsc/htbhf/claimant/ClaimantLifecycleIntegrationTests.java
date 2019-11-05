package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.buildClaimRequestEntity;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aClaimDTOWithClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NO_CHILDREN;

/**
 * Runs a claim through the entire lifecycle, preforming (limited) tests at each payment cycle to confirm the correct
 * amounts have been paid and the correct emails sent.
 */
@SuppressWarnings("PMD.AvoidReassigningParameters")
public class ClaimantLifecycleIntegrationTests extends ScheduledServiceIntegrationTest {

    public static final int CYCLE_DURATION = 28;

    @Autowired
    TestRestTemplate restTemplate;

    /**
     * Run a simple claim through its entire lifecycle and confirm the correct events happen & right emails sent.
     * Each vertical bar is the start of a payment cycle.
     *                                                     | one year
     * |...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|...|
     * | claim starts
     * ..........| Due date & dob ...................................| child's first birthday
     * ..................| New baby reported to DWP
     * ....................| Backdated vouchers paid
     * ........................................................| email about upcoming changes to payment
     */
    @Test
    void shouldProcessClaimFromPregnancyToChildTurningFour() throws JsonProcessingException, NotificationClientException {
        LocalDate expectedDeliveryDate = LocalDate.now().plusDays(70); // 2.5 cycles before due date

        UUID claimId = applyForHealthyStartAsPregnantWomanWithNoChildren(expectedDeliveryDate);
        assertFirstCyclePaidCorrectly(claimId);

        // run through 4 cycles while pregnant (we pay vouchers for up to 12 weeks after due date
        // - their due date is during the 3rd cycle so the claim would expire during 6th if no birth reported)
        // TODO: HTBHD-2377 & HTBHF-1503 reminder email sent at second payment after due date
        expectedDeliveryDate = progressThroughPaymentCyclesForPregnancy(expectedDeliveryDate, claimId, 4);

        // child was born exactly on due date - one and a half cycles ago - notify in time to get backdated vouchers
        List<LocalDate> childrenDob = ageByOneCycle(asList(expectedDeliveryDate));
        assertPaymentCycleWithBackdatedVouchersForNewChild(claimId, childrenDob);
        verifyNoMoreInteractions(notificationClient);

        // run through another 8 cycles until we near the first birthday
        childrenDob = progressThroughRegularPaymentCycles(claimId, childrenDob, 8);

        // should get notification about child turning one in the next cycle
        childrenDob = progressThroughCycleWithUpcomingBirthday(claimId, childrenDob, EmailType.CHILD_TURNS_ONE);

        // run through 13 cycles per year until we near the 4th birthday (13 + 13 + 12 = 38)
        childrenDob = progressThroughRegularPaymentCycles(claimId, childrenDob, 38);

        // should get notification about child turning four in the next cycle
        childrenDob = progressThroughCycleWithUpcomingBirthday(claimId, childrenDob, EmailType.CHILD_TURNS_FOUR);

        // should make a final payment
        childrenDob = progressThroughRegularPaymentCycles(claimId, childrenDob, 1);

        // the claim should have been in ACTIVE status for over 4 years by now (including pregnancy)
        Claim claim = repositoryMediator.loadClaim(claimId);
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        assertThat(claim.getClaimStatusTimestamp()).isBefore(LocalDateTime.now().minusYears(4));

        // should expire the claim
        LocalDateTime now = LocalDateTime.now();
        ageByOneCycle(childrenDob);
        claim = repositoryMediator.loadClaim(claimId);
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.EXPIRED);
        assertThat(claim.getClaimStatusTimestamp()).isAfterOrEqualTo(now);
        assertThat(claim.getCardStatus()).isEqualTo(CardStatus.PENDING_CANCELLATION);
        assertThat(claim.getCardStatusTimestamp()).isAfterOrEqualTo(now);

        // should schedule the card for cancellation after four cycles
        ageByFourCycles(childrenDob);
        claim = repositoryMediator.loadClaim(claimId);
        assertThat(claim.getCardStatus()).isEqualTo(CardStatus.SCHEDULED_FOR_CANCELLATION);
        assertThat(claim.getCardStatusTimestamp()).isAfterOrEqualTo(now);
    }

    private LocalDate progressThroughPaymentCyclesForPregnancy(LocalDate expectedDeliveryDate, UUID claimId, int numCycles)
            throws NotificationClientException, JsonProcessingException {
        for (int i = 0; i < numCycles; i++) {
            resetNotificationClient();
            repositoryMediator.ageDatabaseEntities(CYCLE_DURATION);
            wiremockManager.stubSuccessfulEligibilityResponse(NO_CHILDREN);
            invokeAllSchedulers();
            assertRegularPaymentCyclePaidCorrectly(claimId, NO_CHILDREN);
            verifyNoMoreInteractions(notificationClient);
        }
        return expectedDeliveryDate.minusDays((long) CYCLE_DURATION * numCycles);
    }

    private List<LocalDate> progressThroughCycleWithUpcomingBirthday(UUID claimId, List<LocalDate> childrenDob, EmailType emailType)
            throws NotificationClientException, JsonProcessingException {
        resetNotificationClient();
        childrenDob = ageByOneCycle(childrenDob);
        PaymentCycle paymentCycle = assertRegularPaymentCyclePaidCorrectly(claimId, childrenDob);
        assertEmailSent(emailType, paymentCycle);
        verifyNoMoreInteractions(notificationClient);
        return childrenDob;
    }

    private List<LocalDate> progressThroughRegularPaymentCycles(UUID claimId, List<LocalDate> childrenDob, int numCycles) throws NotificationClientException,
            JsonProcessingException {
        for (int i = 0; i < numCycles; i++) {
            resetNotificationClient();
            childrenDob = ageByOneCycle(childrenDob);
            assertRegularPaymentCyclePaidCorrectly(claimId, childrenDob);
            verifyNoMoreInteractions(notificationClient);
        }
        return childrenDob;
    }

    private UUID applyForHealthyStartAsPregnantWomanWithNoChildren(LocalDate expectedDeliveryDate) throws JsonProcessingException, NotificationClientException {
        String cardAccountId = UUID.randomUUID().toString();
        wiremockManager.stubSuccessfulEligibilityResponse(NO_CHILDREN);
        wiremockManager.stubSuccessfulNewCardResponse(cardAccountId);
        wiremockManager.stubSuccessfulCardBalanceResponse(cardAccountId, 0);
        wiremockManager.stubSuccessfulDepositResponse(cardAccountId);
        stubNotificationEmailResponse();

        ClaimDTO claimDTO = aClaimDTOWithClaimant(aClaimantDTOWithExpectedDeliveryDate(expectedDeliveryDate));
        restTemplate.exchange(buildClaimRequestEntity(claimDTO), ClaimResultDTO.class);
        invokeAllSchedulers();

        ClaimantDTO claimant = claimDTO.getClaimant();
        Claim claim = repositoryMediator.getClaimForNino(claimant.getNino());
        return claim.getId();
    }

    private List<LocalDate> ageByOneCycle(List<LocalDate> initialChildrenDobs) throws JsonProcessingException {
        repositoryMediator.ageDatabaseEntities(CYCLE_DURATION);
        List<LocalDate> newDobs = initialChildrenDobs.stream().map(localDate -> localDate.minusDays(CYCLE_DURATION)).collect(Collectors.toList());
        wiremockManager.stubSuccessfulEligibilityResponse(newDobs);
        invokeAllSchedulers();
        return newDobs;
    }

    private void ageByFourCycles(List<LocalDate> initialChildrenDobs) throws JsonProcessingException {
        for (int i = 0;i < 4;i++) {
            ageByOneCycle(initialChildrenDobs);
        }
    }

    private void assertFirstCyclePaidCorrectly(UUID claimId) throws NotificationClientException {
        Claim claim = repositoryMediator.loadClaim(claimId);
        PaymentCycle paymentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        assertPaymentHasCorrectAmount(claim, paymentCycle, NO_CHILDREN);
        assertThatNewCardEmailSentCorrectly(claim, paymentCycle);
        verifyNoMoreInteractions(notificationClient);
    }

    private PaymentCycle assertRegularPaymentCyclePaidCorrectly(UUID claimId, List<LocalDate> childrenDob) throws NotificationClientException {
        Claim claim = repositoryMediator.loadClaim(claimId);
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        PaymentCycle paymentCycle = getAndAssertPaymentCycle(claim);
        assertPaymentHasCorrectAmount(claim, paymentCycle, childrenDob);
        assertThatPaymentEmailWasSent(paymentCycle);
        return paymentCycle;
    }

    private void assertPaymentCycleWithBackdatedVouchersForNewChild(UUID claimId, List<LocalDate> childrenDob) throws NotificationClientException {
        Claim claim = repositoryMediator.loadClaim(claimId);
        assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        PaymentCycle paymentCycle = getAndAssertPaymentCycle(claim);
        PaymentCycleVoucherEntitlement expectedEntitlement =
                aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild(paymentCycle.getCycleStartDate(), childrenDob);
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedEntitlement);
        assertThat(paymentCycle.getPayments()).isNotEmpty();
        Payment payment = paymentCycle.getPayments().iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedEntitlement.getTotalVoucherValueInPence());
        assertThatNewChildEmailWasSent(paymentCycle);
    }

    private PaymentCycle getAndAssertPaymentCycle(Claim claim) {
        PaymentCycle paymentCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        assertThat(paymentCycle.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.FULL_PAYMENT_MADE);
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(LocalDate.now());
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(LocalDate.now().plusDays(27));
        return paymentCycle;
    }

    private void assertPaymentHasCorrectAmount(Claim claim, PaymentCycle paymentCycle, List<LocalDate> childrenDob) {
        Claimant claimant = claim.getClaimant();
        PaymentCycleVoucherEntitlement expectedEntitlement =
                aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(paymentCycle.getCycleStartDate(), childrenDob, claimant.getExpectedDeliveryDate());
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedEntitlement);
        assertThat(paymentCycle.getPayments()).isNotEmpty();
        Payment payment = paymentCycle.getPayments().iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedEntitlement.getTotalVoucherValueInPence());
    }

    private void assertEmailSent(EmailType emailType, PaymentCycle paymentCycle) throws NotificationClientException {
        verify(notificationClient).sendEmail(eq(emailType.getTemplateId()), eq(paymentCycle.getClaim().getClaimant().getEmailAddress()), any(), any(), any());
    }

    private void invokeAllSchedulers() {
        messageProcessorScheduler.processCreateNewCardMessages();
        messageProcessorScheduler.processFirstPaymentMessages();
        paymentCycleScheduler.createNewPaymentCycles();
        messageProcessorScheduler.processDetermineEntitlementMessages();
        messageProcessorScheduler.processPaymentMessages();
        messageProcessorScheduler.processSendEmailMessages();
        cardCancellationScheduler.handleCardsPendingCancellation();
    }

    private void resetNotificationClient() throws NotificationClientException {
        Mockito.reset(notificationClient);
        stubNotificationEmailResponse();
    }
}
