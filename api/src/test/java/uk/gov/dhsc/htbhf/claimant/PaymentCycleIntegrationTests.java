package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.scheduler.MessageProcessorScheduler;
import uk.gov.dhsc.htbhf.claimant.scheduler.PaymentCycleScheduler;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.dhsc.htbhf.claimant.testsupport.WiremockManager;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.EMAIL_DATE_PATTERN;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.formatVoucherAmount;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlement;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class PaymentCycleIntegrationTests {

    private static final LocalDate SIX_MONTH_OLD = LocalDate.now().minusMonths(6);
    private static final LocalDate THREE_YEAR_OLD = LocalDate.now().minusYears(3);

    @MockBean
    private NotificationClient notificationClient;
    private SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);

    @Autowired
    private PaymentCycleScheduler paymentCycleScheduler;
    @Autowired
    private MessageProcessorScheduler messageProcessorScheduler;
    @Autowired
    private RepositoryMediator repositoryMediator;
    @Autowired
    private WiremockManager wiremockManager;

    @BeforeEach
    void setup() {
        wiremockManager.startWireMock();
    }

    @AfterEach
    void tearDown() {
        repositoryMediator.deleteAllEntities();
        wiremockManager.stopWireMock();
    }

    @Test
    void shouldCreatePaymentCycleMakePaymentAndSendEmail() throws JsonProcessingException, NotificationClientException {
        // setup some claim variables
        String cardAccountId = UUID.randomUUID().toString();
        List<LocalDate> sixMonthOldAndThreeYearOld = Arrays.asList(SIX_MONTH_OLD, THREE_YEAR_OLD);
        int cardBalanceInPenceBeforeDeposit = 88;

        wiremockManager.stubSuccessfulEligibilityResponse(sixMonthOldAndThreeYearOld);
        wiremockManager.stubCardBalanceAndDepositResponses(cardAccountId, cardBalanceInPenceBeforeDeposit);
        stubNotificationEmailResponse();

        Claim claim = createClaimWithPaymentCycleEndingYesterday(cardAccountId, sixMonthOldAndThreeYearOld, LocalDate.now().plusMonths(4));

        invokeAllSchedulers();

        // confirm new payment cycle created with a payment
        PaymentCycle newCycle = repositoryMediator.getCurrentPaymentCycleForClaim(claim);
        PaymentCycleVoucherEntitlement expectedVoucherEntitlement =
                aPaymentCycleVoucherEntitlement(LocalDate.now(), sixMonthOldAndThreeYearOld, claim.getClaimant().getExpectedDeliveryDate());
        assertPaymentCycleIsIsFullyPaid(newCycle, sixMonthOldAndThreeYearOld, cardBalanceInPenceBeforeDeposit, expectedVoucherEntitlement);

        // confirm card service called to make payment
        Payment payment = newCycle.getPayments().iterator().next();
        wiremockManager.assertThatGetBalanceAndDepositFundsInvokedForPayment(payment);

        // confirm notify component invoked with correct email template & personalisation
        assertThatPaymentEmailWasSent(newCycle);
    }

    private void stubNotificationEmailResponse() throws NotificationClientException {
        when(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenReturn(sendEmailResponse);
    }

    private void invokeAllSchedulers() {
        paymentCycleScheduler.createNewPaymentCycles();
        messageProcessorScheduler.processDetermineEntitlementMessages();
        messageProcessorScheduler.processPaymentMessages();
        messageProcessorScheduler.processSendEmailMessages();
    }

    private void assertPaymentCycleIsIsFullyPaid(PaymentCycle paymentCycle, List<LocalDate> childrensDatesOfBirth,
                                                 int cardBalanceInPenceBeforeDeposit, PaymentCycleVoucherEntitlement expectedVoucherEntitlement) {
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(LocalDate.now());
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(LocalDate.now().plusDays(27));
        assertThat(paymentCycle.getChildrenDob()).isEqualTo(childrensDatesOfBirth);
        assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(expectedVoucherEntitlement);
        assertThat(paymentCycle.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.FULL_PAYMENT_MADE);
        assertThat(paymentCycle.getCardBalanceInPence()).isEqualTo(cardBalanceInPenceBeforeDeposit);
        assertThat(paymentCycle.getTotalEntitlementAmountInPence()).isEqualTo(expectedVoucherEntitlement.getTotalVoucherValueInPence());
        assertThat(paymentCycle.getPayments()).hasSize(1);
        Payment payment = paymentCycle.getPayments().iterator().next();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(expectedVoucherEntitlement.getTotalVoucherValueInPence());

    }

    private void assertThatPaymentEmailWasSent(PaymentCycle newCycle) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(EmailType.PAYMENT.getTemplateId()), eq(newCycle.getClaim().getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(), any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertThat(personalisationMap).isNotNull();
        Claim claim = newCycle.getClaim();
        Claimant claimant = claim.getClaimant();
        PaymentCycleVoucherEntitlement entitlement = newCycle.getVoucherEntitlement();
        assertThat(personalisationMap.get(EmailTemplateKey.FIRST_NAME.getTemplateKeyName())).isEqualTo(claimant.getFirstName());
        assertThat(personalisationMap.get(EmailTemplateKey.LAST_NAME.getTemplateKeyName())).isEqualTo(claimant.getLastName());
        assertThat(personalisationMap.get(EmailTemplateKey.PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(newCycle.getTotalVouchers()));
        assertThat(personalisationMap.get(EmailTemplateKey.CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenUnderOne()));
        assertThat(personalisationMap.get(EmailTemplateKey.CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenBetweenOneAndFour()));
        assertThat(personalisationMap.get(EmailTemplateKey.PREGNANCY_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForPregnancy()));
        assertThat(personalisationMap.get(EmailTemplateKey.NEXT_PAYMENT_DATE.getTemplateKeyName())).asString()
                .contains(newCycle.getCycleEndDate().plusDays(1).format(EMAIL_DATE_PATTERN));
    }

    private Claim createClaimWithPaymentCycleEndingYesterday(String cardAccountId, List<LocalDate> childrensDatesOfBirth, LocalDate expectedDeliveryDate) {
        Claim claim = aValidClaimBuilder()
                .claimant(aClaimantWithExpectedDeliveryDate(expectedDeliveryDate))
                .cardAccountId(cardAccountId)
                .build();
        repositoryMediator.createAndSavePaymentCycle(claim, LocalDate.now().minusDays(28), childrensDatesOfBirth);
        return claim;
    }

}


