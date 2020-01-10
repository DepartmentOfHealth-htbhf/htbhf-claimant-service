package uk.gov.dhsc.htbhf.claimant;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;
import uk.gov.dhsc.htbhf.claimant.scheduler.CardCancellationScheduler;
import uk.gov.dhsc.htbhf.claimant.scheduler.MessageProcessorScheduler;
import uk.gov.dhsc.htbhf.claimant.scheduler.PaymentCycleScheduler;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;
import uk.gov.dhsc.htbhf.claimant.testsupport.WiremockManager;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;

import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.EMAIL_DATE_PATTERN;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.formatVoucherAmount;
import static uk.gov.dhsc.htbhf.claimant.communications.MessagePayloadUtils.buildPregnancyPaymentAmountSummary;
import static uk.gov.dhsc.htbhf.claimant.communications.MessagePayloadUtils.buildUnder1PaymentSummary;
import static uk.gov.dhsc.htbhf.claimant.communications.MessagePayloadUtils.buildUnder4PaymentSummary;
import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.*;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.*;

/**
 * Contains commons methods that can be used as a part of integration tests.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
abstract class ScheduledServiceIntegrationTest {

    @Autowired
    RepositoryMediator repositoryMediator;
    @Autowired
    WiremockManager wiremockManager;
    @Autowired
    PaymentCycleScheduler paymentCycleScheduler;
    @Autowired
    MessageProcessorScheduler messageProcessorScheduler;
    @Autowired
    CardCancellationScheduler cardCancellationScheduler;

    @MockBean
    NotificationClient notificationClient;
    @Mock
    private SendEmailResponse sendEmailResponse;
    @Mock
    private SendLetterResponse sendLetterResponse;

    @BeforeEach
    void setup() {
        wiremockManager.startWireMock();
    }

    @AfterEach
    void tearDown() {
        repositoryMediator.deleteAllEntities();
        wiremockManager.stopWireMock();
    }

    void stubNotificationEmailResponse() throws NotificationClientException {
        when(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenReturn(sendEmailResponse);
    }

    void stubNotificationLetterResponse() throws NotificationClientException {
        when(notificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
    }

    void stubNotificationEmailError() throws NotificationClientException {
        when(notificationClient.sendEmail(any(), any(), any(), any(), any())).thenThrow(new NotificationClientException("Something went wrong"));
    }

    void assertThatChildTurnsOneInFirstWeekEmailWasSent(PaymentCycle currentCycle) throws NotificationClientException {
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(CHILD_TURNS_ONE.getTemplateId()), eq(currentCycle.getClaim().getClaimant().getEmailAddress()), argumentCaptor.capture(), any(), any());
        assertChildTurnsOneInFirstWeekEmailPersonalisationMap(currentCycle, argumentCaptor.getValue());
    }

    void assertThatChildTurnsOneOnFirstDayEmailWasSent(PaymentCycle currentCycle) throws NotificationClientException {
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(CHILD_TURNS_ONE.getTemplateId()), eq(currentCycle.getClaim().getClaimant().getEmailAddress()), argumentCaptor.capture(), any(), any());
        assertChildTurnsOneOnFirstDayEmailPersonalisationMap(currentCycle, argumentCaptor.getValue());
    }

    void assertThatChildTurnsFourEmailWasSent(PaymentCycle currentCycle) throws NotificationClientException {
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(CHILD_TURNS_FOUR.getTemplateId()), eq(currentCycle.getClaim().getClaimant().getEmailAddress()), argumentCaptor.capture(), any(), any());
        assertChildTurnsFourEmailPersonalisationMap(currentCycle, argumentCaptor.getValue());
    }

    void assertThatPaymentStoppingEmailWasSent(PaymentCycle currentCycle) throws NotificationClientException {
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(PAYMENT_STOPPING.getTemplateId()), eq(currentCycle.getClaim().getClaimant().getEmailAddress()), argumentCaptor.capture(), any(), any());
        assertYoungestChildTurnsFourEmailPersonalisationMap(argumentCaptor.getValue());
    }

    void assertThatRegularPaymentEmailWasSent(PaymentCycle newCycle) throws NotificationClientException {
        assertThatPaymentEmailWasSent(newCycle, EmailType.REGULAR_PAYMENT);
    }

    void assertThatPaymentEmailWasSent(PaymentCycle newCycle, EmailType emailType) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(emailType.getTemplateId()), eq(newCycle.getClaim().getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(), any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertPaymentEmailPersonalisationMap(newCycle, personalisationMap);
    }

    void assertThatReportABirthReminderEmailWasSent(Claim claim) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(REPORT_A_BIRTH_REMINDER.getTemplateId()), eq(claim.getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(), any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertNameEmailFields(claim, personalisationMap);
    }

    void assertThatNewChildEmailWasSent(PaymentCycle newCycle) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(NEW_CHILD_FROM_PREGNANCY.getTemplateId()), eq(newCycle.getClaim().getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(),
                any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertPaymentEmailPersonalisationMap(newCycle, personalisationMap);
        assertThat(personalisationMap.get(BACKDATED_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(newCycle.getVoucherEntitlement().getBackdatedVouchers()));
    }

    void assertThatNoChildOnFeedNoLongerEligibleEmailWasSent(Claim claim) throws NotificationClientException {
        assertThatEmailWithNameOnlyWasSent(claim, NO_CHILD_ON_FEED_NO_LONGER_ELIGIBLE);
    }

    void assertThatClaimNoLongerEligibleEmailWasSent(Claim claim) throws NotificationClientException {
        assertThatEmailWithNameOnlyWasSent(claim, CLAIM_NO_LONGER_ELIGIBLE);
    }

    void assertThatInstantSuccessEmailSentCorrectly(Claim claim, PaymentCycle paymentCycle) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(EmailType.INSTANT_SUCCESS.getTemplateId()), eq(claim.getClaimant().getEmailAddress()), mapArgumentCaptor.capture(), any(), any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertNameEmailFields(claim, personalisationMap);
        assertCommonPaymentEmailFields(paymentCycle, personalisationMap);
        assertThat(personalisationMap.get(EmailTemplateKey.PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(paymentCycle.getTotalVouchers()));
        PaymentCycleVoucherEntitlement entitlement = paymentCycle.getVoucherEntitlement();
        assertThat(personalisationMap.get(EmailTemplateKey.CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenUnderOne()));
        assertThat(personalisationMap.get(EmailTemplateKey.CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenBetweenOneAndFour()));
    }

    void assertThatCardIsAboutToBeCancelledEmailWasSent(Claim claim) throws NotificationClientException {
        assertThatEmailWithNameOnlyWasSent(claim, CARD_IS_ABOUT_TO_BE_CANCELLED);
    }

    void assertThatEmailWithNameOnlyWasSent(Claim claim, EmailType emailType) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(
                eq(emailType.getTemplateId()),
                eq(claim.getClaimant().getEmailAddress()),
                mapArgumentCaptor.capture(),
                any(),
                any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertThat(personalisationMap).hasSize(2);
        assertNameEmailFields(claim, personalisationMap);
    }

    void assertThatLetterWithAddressOnlyWasSent(Claim claim, LetterType letterType) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendLetter(
                eq(letterType.getTemplateId()),
                mapArgumentCaptor.capture(),
                any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertThatMapContainsLetterAddressFieldsOnly(claim, personalisationMap);
    }

    void assertThatLetterWithAddressAndPaymentFieldsWasSent(Claim claim, PaymentCycle paymentCycle, LetterType letterType) throws NotificationClientException {
        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendLetter(
                eq(letterType.getTemplateId()),
                mapArgumentCaptor.capture(),
                any());

        Map personalisationMap = mapArgumentCaptor.getValue();
        assertThatMapContainsAddressAndPaymentFields(claim, paymentCycle, personalisationMap);
    }

    protected void assertThatMapContainsAddressAndPaymentFields(Claim claim, PaymentCycle paymentCycle, Map personalisationMap) {
        assertThat(personalisationMap).contains(addressEntries(claim.getClaimant()));
        PaymentCycleVoucherEntitlement voucherEntitlement = paymentCycle.getVoucherEntitlement();
        assertThat(personalisationMap).contains(
                entry("payment_amount", formatVoucherAmount(voucherEntitlement.getTotalVoucherEntitlement())),
                entry("pregnancy_payment", buildPregnancyPaymentAmountSummary(voucherEntitlement)),
                entry("children_under_1_payment", buildUnder1PaymentSummary(voucherEntitlement)),
                entry("children_under_4_payment", buildUnder4PaymentSummary(voucherEntitlement))
        );
    }

    private void assertThatMapContainsLetterAddressFieldsOnly(Claim claim, Map personalisationMap) {
        assertThat(personalisationMap).containsOnly(addressEntries(claim.getClaimant()));
    }

    private Map.Entry[] addressEntries(Claimant claimant) {
        Address address = claimant.getAddress();
        return new Map.Entry[]{
                entry("address_line_1", claimant.getFirstName() + " " + claimant.getLastName()),
                entry("address_line_2", address.getAddressLine1()),
                entry("address_line_3", address.getAddressLine2()),
                entry("address_line_4", address.getTownOrCity()),
                entry("address_line_5", address.getCounty()),
                entry("postcode", address.getPostcode())};
    }

    private void assertChildTurnsOneInFirstWeekEmailPersonalisationMap(PaymentCycle currentCycle, Map personalisationMap) {
        assertCommonPaymentEmailFields(currentCycle, personalisationMap);
        assertThat(personalisationMap.get(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(0)); // child is turning one in the next payment cycle, so no vouchers going forward
        assertThat(personalisationMap.get(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // child is turning one in the next payment cycle, so one voucher per week going forward
        assertThat(personalisationMap.get(PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(5)); // two vouchers in first week plus one for weeks two, three and four
        assertThat(personalisationMap.get(EmailTemplateKey.REGULAR_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // child is turning one in the next payment cycle, so one voucher per week going forward
    }

    private void assertChildTurnsOneOnFirstDayEmailPersonalisationMap(PaymentCycle currentCycle, Map personalisationMap) {
        assertCommonPaymentEmailFields(currentCycle, personalisationMap);
        assertThat(personalisationMap.get(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(0)); // child is turning one in the next payment cycle, so no vouchers going forward
        assertThat(personalisationMap.get(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // child is turning one in the next payment cycle, so one voucher per week going forward
        assertThat(personalisationMap.get(PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(4)); // one voucher each week
        assertThat(personalisationMap.get(EmailTemplateKey.REGULAR_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // child is turning one in the next payment cycle, so one voucher per week going forward
    }

    private void assertChildTurnsFourEmailPersonalisationMap(PaymentCycle currentCycle, Map personalisationMap) {
        assertCommonPaymentEmailFields(currentCycle, personalisationMap);
        assertThat(personalisationMap.get(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(0)); // no children under one
        assertThat(personalisationMap.get(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // only one child will be under four
        assertThat(personalisationMap.get(PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(5)); // four vouchers for the three year old and one voucher in first week only for child turning four
        assertThat(personalisationMap.get(EmailTemplateKey.REGULAR_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(4)); // child has turned four, so no vouchers going forward
    }

    private void assertYoungestChildTurnsFourEmailPersonalisationMap(Map personalisationMap) {
        assertThat(personalisationMap.get(PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(4)); // one voucher each week
        assertThat(personalisationMap.get(EmailTemplateKey.MULTIPLE_CHILDREN.getTemplateKeyName()))
                .isEqualTo(false); // only one child turning four
    }

    private void assertPaymentEmailPersonalisationMap(PaymentCycle newCycle, Map personalisationMap) {
        PaymentCycleVoucherEntitlement entitlement = newCycle.getVoucherEntitlement();
        assertCommonPaymentEmailFields(newCycle, personalisationMap);
        assertThat(personalisationMap.get(PAYMENT_AMOUNT.getTemplateKeyName()))
                .isEqualTo(formatVoucherAmount(newCycle.getTotalVouchers()));
        assertThat(personalisationMap.get(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenUnderOne()));
        assertThat(personalisationMap.get(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getVouchersForChildrenBetweenOneAndFour()));
        assertThat(personalisationMap.get(EmailTemplateKey.REGULAR_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(entitlement.getLastVoucherEntitlementForCycle().getTotalVoucherEntitlement() * 4));
    }

    private void assertCommonPaymentEmailFields(PaymentCycle newCycle, Map personalisationMap) {
        assertNameEmailFields(newCycle.getClaim(), personalisationMap);
        assertThat(personalisationMap.get(PREGNANCY_PAYMENT.getTemplateKeyName())).asString()
                .contains(formatVoucherAmount(newCycle.getVoucherEntitlement().getVouchersForPregnancy()));
        assertThat(personalisationMap.get(NEXT_PAYMENT_DATE.getTemplateKeyName())).asString()
                .contains(newCycle.getCycleEndDate().plusDays(1).format(EMAIL_DATE_PATTERN));
    }

    private void assertNameEmailFields(Claim claim, Map personalisationMap) {
        Claimant claimant = claim.getClaimant();
        assertThat(personalisationMap).isNotNull();
        assertThat(personalisationMap.get(FIRST_NAME.getTemplateKeyName())).isEqualTo(claimant.getFirstName());
        assertThat(personalisationMap.get(LAST_NAME.getTemplateKeyName())).isEqualTo(claimant.getLastName());
    }

}
