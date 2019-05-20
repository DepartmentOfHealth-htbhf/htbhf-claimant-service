package uk.gov.dhsc.htbhf.claimant.message.processor;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.CycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aDetermineEntitlementMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aMakePaymentPayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleStartDateAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleStartDateEntitlementAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aValidPaymentCycleVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.LISA_DOB;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.MAGGIE_DOB;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class DetermineEntitlementMessageProcessorTest {

    @Mock
    private EligibilityService eligibilityService;
    @Mock
    private CycleEntitlementCalculator cycleEntitlementCalculator;
    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private PaymentCycleService paymentCycleService;
    @Mock
    private MessageQueueDAO messageQueueDAO;

    @InjectMocks
    private DetermineEntitlementMessageProcessor processor;

    @Test
    void shouldSuccessfullyProcessMessageAndTriggerPaymentForEligibleClaimant() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext();
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(ELIGIBLE);
        given(eligibilityService.determineEligibilityForExistingClaimant(any())).willReturn(eligibilityResponse);

        //Current payment cycle voucher entitlement mocking
        PaymentCycleVoucherEntitlement currentPaymentCycleVoucherEntitlement = aValidPaymentCycleVoucherEntitlement();
        given(cycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(currentPaymentCycleVoucherEntitlement);
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityService).determineEligibilityForExistingClaimant(context.getClaim().getClaimant());

        List<LocalDate> dateOfBirthOfChildren = ImmutableList.of(MAGGIE_DOB, LISA_DOB);
        verify(cycleEntitlementCalculator).calculateEntitlement(
                Optional.of(context.getClaim().getClaimant().getExpectedDeliveryDate()),
                dateOfBirthOfChildren,
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle().getVoucherEntitlement());

        verify(paymentCycleService).updateAndSavePaymentCycle(context.getCurrentPaymentCycle(), ELIGIBLE, currentPaymentCycleVoucherEntitlement);
        MakePaymentMessagePayload expectedPaymentMessagePayload = aMakePaymentPayload(context.getClaim().getId(), context.getCurrentPaymentCycle().getId());
        verify(messageQueueDAO).sendMessage(expectedPaymentMessagePayload, MAKE_PAYMENT);
    }

    @Test
    void shouldProcessMessageButNotTriggerPaymentForClaimantNoLongerEligible() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext();
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(INELIGIBLE);
        given(eligibilityService.determineEligibilityForExistingClaimant(any())).willReturn(eligibilityResponse);

        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityService).determineEligibilityForExistingClaimant(context.getClaim().getClaimant());

        verify(paymentCycleService).updateAndSavePaymentCycle(context.getCurrentPaymentCycle(), INELIGIBLE, null);
        verifyZeroInteractions(cycleEntitlementCalculator, messageQueueDAO);
    }

    private DetermineEntitlementMessageContext buildMessageContext() {
        //Claim
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(1);
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);

        //Current payment cycle
        LocalDate cycleStartDate = LocalDate.now();
        PaymentCycle currentPaymentCycle = aPaymentCycleWithCycleStartDateAndClaim(cycleStartDate, claim);

        //Previous payment cycle
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        VoucherEntitlement previousVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(previousCycleStartDate);
        PaymentCycleVoucherEntitlement previousPaymentCycleVoucherEntitlement = aPaymentCycleVoucherEntitlementWithEntitlement(previousVoucherEntitlement);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithCycleStartDateEntitlementAndClaim(previousCycleStartDate,
                previousPaymentCycleVoucherEntitlement,
                claim);

        return aDetermineEntitlementMessageContext(
                currentPaymentCycle,
                previousPaymentCycle,
                claim);
    }

}
