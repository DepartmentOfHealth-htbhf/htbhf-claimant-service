package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.communications.DetermineEntitlementNotificationHandler;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.QualifyingBenefitEligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.QualifyingBenefitEligibilityStatus.CONFIRMED;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.QualifyingBenefitEligibilityStatus.NOT_CONFIRMED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDateAndChildrenDobs;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aDetermineEntitlementMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartDateAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class DetermineEntitlementMessageProcessorTest {

    @Mock
    private EligibilityAndEntitlementService eligibilityAndEntitlementService;
    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private PaymentCycleService paymentCycleService;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private DetermineEntitlementNotificationHandler determineEntitlementNotificationHandler;
    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;

    @InjectMocks
    private DetermineEntitlementMessageProcessor processor;

    @Test
    void shouldSuccessfullyProcessMessageAndTriggerPaymentWhenClaimantIsEligible() {
        //Given
        DetermineEntitlementMessageContext context = buildMessageContext(
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                TWO_CHILDREN,
                TWO_CHILDREN);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility response with children returned
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateExistingClaimant(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verify(paymentCycleService).updatePaymentCycle(context.getCurrentPaymentCycle(), decision);
        MessagePayload expectedPayload = MessagePayloadFactory.buildMakePaymentMessagePayload(context.getCurrentPaymentCycle());
        verify(messageQueueClient).sendMessage(expectedPayload, MessageType.MAKE_PAYMENT);
        verifyZeroInteractions(claimRepository, pregnancyEntitlementCalculator, childDateOfBirthCalculator, childDateOfBirthCalculator);
    }

    @ParameterizedTest(name = "Qualifying benefit status={0}")
    @ValueSource(strings = {"CONFIRMED", "NOT_CONFIRMED"})
    void shouldExpireClaimAndSendNoLongerOnSchemeEmailWhenEligibleWithNoChildrenAndNotPregnant(
            QualifyingBenefitEligibilityStatus qualifyingBenefitEligibilityStatus) {
        //Given
        //Test for HTBHF-2182 has the following context:
        // Previous cycle: children exist and are under 4 but will be 4 in the next cycle, not pregnant
        // Current cycle: no children and not pregnant
        LocalDate previousCycleExpectedDeliveryDate = NOT_PREGNANT;
        LocalDate currentCycleExpectedDeliveryDate = NOT_PREGNANT;
        List<LocalDate> previousCycleChildrenDobs = SINGLE_THREE_YEAR_OLD;
        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        DetermineEntitlementMessageContext context = buildMessageContext(
                previousCycleExpectedDeliveryDate,
                currentCycleExpectedDeliveryDate,
                previousCycleChildrenDobs,
                currentCycleChildrenDobs);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Claimant has children under 4 at the previous payment cycle that would still over 4 now (but not reported by DWP).
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(true);
        given(childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(any(), any())).willReturn(false);

        //Eligibility is INELIGIBLE as they have no children or pregnancy but DWP have returned that they are eligible (CONFIRMED)
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, qualifyingBenefitEligibilityStatus, currentCycleChildrenDobs);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        runCommonVerifications(context, decision, message, messageStatus, ClaimStatus.EXPIRED);
        LocalDate currentPaymentCycleStartDate = context.getCurrentPaymentCycle().getCycleStartDate();
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(NOT_PREGNANT, currentPaymentCycleStartDate);
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(context.getPreviousPaymentCycle());
        verify(childDateOfBirthCalculator).hadChildrenUnderFourAtGivenDate(SINGLE_THREE_YEAR_OLD, currentPaymentCycleStartDate);
        verifyZeroInteractions(messageQueueClient, determineEntitlementNotificationHandler);
    }

    @Test
    void shouldExpireClaimAndSendNoLongerOnSchemeEmailWhenEligibleWithNoChildrenAndNotPregnant() {
        //Given
        //Test for HTBHF-2185 has the following context:
        // Previous cycle: children exist and are under 4, not pregnant
        // Current cycle: no children and not pregnant
        LocalDate previousCycleExpectedDeliveryDate = NOT_PREGNANT;
        LocalDate currentCycleExpectedDeliveryDate = NOT_PREGNANT;
        List<LocalDate> previousCycleChildrenDobs = SINGLE_THREE_YEAR_OLD;
        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        DetermineEntitlementMessageContext context = buildMessageContext(
                previousCycleExpectedDeliveryDate,
                currentCycleExpectedDeliveryDate,
                previousCycleChildrenDobs,
                currentCycleChildrenDobs);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Claimant has children under 4 at the previous payment cycle that would still be under 4 now (but not reported by DWP).
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(true);
        given(childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(any(), any())).willReturn(true);

        //Eligibility is INELIGIBLE as they have no children or pregnancy but DWP have returned that they are eligible (CONFIRMED)
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, CONFIRMED, currentCycleChildrenDobs);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        runCommonVerifications(context, decision, message, messageStatus, ClaimStatus.EXPIRED);
        verify(determineEntitlementNotificationHandler).sendNoChildrenOnFeedClaimNoLongerEligibleEmail(context.getClaim());
        LocalDate currentPaymentCycleStartDate = context.getCurrentPaymentCycle().getCycleStartDate();
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(NOT_PREGNANT, currentPaymentCycleStartDate);
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(context.getPreviousPaymentCycle());
        verify(childDateOfBirthCalculator).hadChildrenUnderFourAtGivenDate(SINGLE_THREE_YEAR_OLD, currentPaymentCycleStartDate);
        verifyZeroInteractions(messageQueueClient);
    }

    //HTBHF-1757 Children in current cycle, DWP returns ineligible, varying on pregnancy and children in previous cycle
    @ParameterizedTest(name = "Children DOB previous cycle={0}, expected delivery date previous cycle={1}, expected delivery date current cycle={2}")
    @MethodSource("provideArgumentsForClaimantBecomingPregnantWithChildrenInCurrentCycle")
    void shouldUpdateClaimToPendingExpiryAndSendNoLongerEligibleEmailWhenClaimantIsNotEligibleWithChildrenInCurrentCycle(
            List<LocalDate> previousCycleChildrenDobs,
            LocalDate previousCycleExpectedDeliveryDate,
            LocalDate currentCycleExpectedDeliveryDate) {
        //Given
        List<LocalDate> currentCycleChildrenDobs = TWO_CHILDREN;
        DetermineEntitlementMessageContext context = buildMessageContext(
                previousCycleExpectedDeliveryDate,
                currentCycleExpectedDeliveryDate,
                previousCycleChildrenDobs,
                currentCycleChildrenDobs);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);

        //Eligibility - any status that isn't ELIGIBLE has the same effect here.
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, NOT_CONFIRMED, currentCycleChildrenDobs);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        runCommonVerifications(context, decision, message, messageStatus, ClaimStatus.PENDING_EXPIRY);
        verify(determineEntitlementNotificationHandler).sendClaimNoLongerEligibleEmail(context.getClaim());
        verifyZeroInteractions(pregnancyEntitlementCalculator, childDateOfBirthCalculator);
    }

    //HTBHF-1757 No children in current cycle, are pregnant but ineligible from DWP
    @ParameterizedTest(name = "Children DOB in previous cycle={0}")
    @MethodSource("provideArgumentsForChildrenInPreviousCycle")
    void shouldUpdateClaimToPendingExpiryAndSendNoLongerEligibleEmailWhenClaimantIsNotEligibleWithNoChildrenInCurrentCycle(
            List<LocalDate> previousCycleChildrenDobs) {
        //Given
        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        LocalDate currentCycleExpectedDeliveryDate = EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
        LocalDate previousCycleExpectedDeliveryDate = EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
        DetermineEntitlementMessageContext context = buildMessageContext(
                previousCycleExpectedDeliveryDate,
                currentCycleExpectedDeliveryDate,
                previousCycleChildrenDobs,
                currentCycleChildrenDobs);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(true);

        //Eligibility - any status that isn't ELIGIBLE has the same effect here.
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, NOT_CONFIRMED, currentCycleChildrenDobs);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        runCommonVerifications(context, decision, message, messageStatus, ClaimStatus.PENDING_EXPIRY);
        verify(determineEntitlementNotificationHandler).sendClaimNoLongerEligibleEmail(context.getClaim());
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, context.getCurrentPaymentCycle().getCycleStartDate());
        verifyZeroInteractions(childDateOfBirthCalculator);
    }

    //HTBHF-1757 Claimant had children in previous cycle who would be under 4 in this cycle but are not returned from DWP
    @Test
    void shouldUpdateClaimToPendingExpiryAndSendNoLongerEligibleEmailWhenClaimantIsNotEligibleWithNoChildrenInCurrentCycle() {
        //Given
        List<LocalDate> previousCycleChildrenDobs = TWO_CHILDREN;
        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        LocalDate currentCycleExpectedDeliveryDate = NOT_PREGNANT;
        LocalDate previousCycleExpectedDeliveryDate = NOT_PREGNANT;
        DetermineEntitlementMessageContext context = buildMessageContext(
                previousCycleExpectedDeliveryDate,
                currentCycleExpectedDeliveryDate,
                previousCycleChildrenDobs,
                currentCycleChildrenDobs);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);
        //Had children in last cycle who are under 4 at the start of the current payment cycle.
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(true);
        given(childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(any(), any())).willReturn(true);

        //Eligibility - any status that isn't ELIGIBLE has the same effect here.
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, NOT_CONFIRMED, currentCycleChildrenDobs);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        runCommonVerifications(context, decision, message, messageStatus, ClaimStatus.PENDING_EXPIRY);
        verify(determineEntitlementNotificationHandler).sendClaimNoLongerEligibleEmail(context.getClaim());
        LocalDate currentPaymentCycleStartDate = context.getCurrentPaymentCycle().getCycleStartDate();
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(NOT_PREGNANT, currentPaymentCycleStartDate);
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(context.getPreviousPaymentCycle());
        verify(childDateOfBirthCalculator).hadChildrenUnderFourAtGivenDate(TWO_CHILDREN, currentPaymentCycleStartDate);
        verifyZeroInteractions(childDateOfBirthCalculator);
    }

    @ParameterizedTest(name = "Qualifying benefit status={0}")
    @ValueSource(strings = {"CONFIRMED", "NOT_CONFIRMED"})
    void shouldExpireClaimWhenClaimantWasPregnantButNoLongerWithChildrenInPreviousCycle(
            QualifyingBenefitEligibilityStatus qualifyingBenefitEligibilityStatus) {
        //Given
        List<LocalDate> previousCycleChildrenDobs = SINGLE_THREE_YEAR_OLD;
        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        LocalDate previousCycleExpectedDeliveryDate = EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
        LocalDate currentCycleExpectedDeliveryDate = NOT_PREGNANT;
        DetermineEntitlementMessageContext context = buildMessageContext(
                previousCycleExpectedDeliveryDate,
                currentCycleExpectedDeliveryDate,
                previousCycleChildrenDobs,
                currentCycleChildrenDobs);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);
        given(pregnancyEntitlementCalculator.isEntitledToVoucher(any(), any())).willReturn(false);
        //Had children in last cycle who are under 4 at the start of the current payment cycle.
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(true);
        //Children in previous cycle are now be over 4.
        given(childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(any(), any())).willReturn(false);

        //Eligibility - any status that isn't ELIGIBLE has the same effect here.
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, qualifyingBenefitEligibilityStatus, currentCycleChildrenDobs);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        runCommonVerifications(context, decision, message, messageStatus, ClaimStatus.EXPIRED);
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(context.getPreviousPaymentCycle());
        verify(pregnancyEntitlementCalculator).isEntitledToVoucher(NOT_PREGNANT, context.getCurrentPaymentCycle().getCycleStartDate());
        verifyZeroInteractions(determineEntitlementNotificationHandler);
    }

    @ParameterizedTest(name = "Qualifying benefit status={0}")
    @ValueSource(strings = {"CONFIRMED", "NOT_CONFIRMED"})
    void shouldExpireClaimWhenClaimantWasPregnantButNoLongerWithNoChildrenInPreviousCycle(
            QualifyingBenefitEligibilityStatus qualifyingBenefitEligibilityStatus) {
        //Given
        List<LocalDate> previousCycleChildrenDobs = NO_CHILDREN;
        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        LocalDate previousCycleExpectedDeliveryDate = EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
        LocalDate currentCycleExpectedDeliveryDate = NOT_PREGNANT;
        DetermineEntitlementMessageContext context = buildMessageContext(
                previousCycleExpectedDeliveryDate,
                currentCycleExpectedDeliveryDate,
                previousCycleChildrenDobs,
                currentCycleChildrenDobs);
        given(messageContextLoader.loadDetermineEntitlementContext(any())).willReturn(context);
        LocalDate currentPaymentCycleStartDate = context.getCurrentPaymentCycle().getCycleStartDate();
        LocalDate previousPaymentCycleStartDate = context.getPreviousPaymentCycle().getCycleStartDate();
        lenient().when(pregnancyEntitlementCalculator.isEntitledToVoucher(NOT_PREGNANT, currentPaymentCycleStartDate)).thenReturn(false);
        lenient().when(pregnancyEntitlementCalculator.isEntitledToVoucher(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, previousPaymentCycleStartDate))
                .thenReturn(true);
        //Had children in last cycle who are under 4 at the start of the current payment cycle.
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(false);

        //Eligibility - any status that isn't ELIGIBLE has the same effect here.
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, qualifyingBenefitEligibilityStatus, currentCycleChildrenDobs);
        given(eligibilityAndEntitlementService.evaluateExistingClaimant(any(), any(), any())).willReturn(decision);

        //Current payment cycle voucher entitlement mocking
        Message message = aValidMessageWithType(DETERMINE_ENTITLEMENT);

        //When
        MessageStatus messageStatus = processor.processMessage(message);

        //Then
        runCommonVerifications(context, decision, message, messageStatus, ClaimStatus.EXPIRED);
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(context.getPreviousPaymentCycle());
        InOrder inOrder = inOrder(pregnancyEntitlementCalculator, pregnancyEntitlementCalculator);
        inOrder.verify(pregnancyEntitlementCalculator).isEntitledToVoucher(NOT_PREGNANT, currentPaymentCycleStartDate);
        inOrder.verify(pregnancyEntitlementCalculator).isEntitledToVoucher(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, previousPaymentCycleStartDate);
        verifyZeroInteractions(determineEntitlementNotificationHandler);
    }

    private void verifyClaimSavedAtStatus(ClaimStatus claimStatus) {
        ArgumentCaptor<Claim> argumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(argumentCaptor.capture());
        Claim claim = argumentCaptor.getValue();
        assertThat(claim.getClaimStatus()).isEqualTo(claimStatus);
    }

    private void runCommonVerifications(DetermineEntitlementMessageContext context, EligibilityAndEntitlementDecision decision,
                                        Message message, MessageStatus messageStatus, ClaimStatus expectedClaimStatus) {
        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadDetermineEntitlementContext(message);
        verify(eligibilityAndEntitlementService).evaluateExistingClaimant(context.getClaim().getClaimant(),
                context.getCurrentPaymentCycle().getCycleStartDate(),
                context.getPreviousPaymentCycle());

        verify(paymentCycleService).updatePaymentCycle(context.getCurrentPaymentCycle(), decision);
        verifyClaimSavedAtStatus(expectedClaimStatus);
    }

    private DetermineEntitlementMessageContext buildMessageContext(LocalDate previousCycleExpectedDeliveryDate,
                                                                   LocalDate currentCycleExpectedDeliveryDate,
                                                                   List<LocalDate> previousCycleChildrenDobs,
                                                                   List<LocalDate> currentCycleChildrenDobs) {
        //Claim
        Claim claimAtPreviousCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(previousCycleExpectedDeliveryDate, previousCycleChildrenDobs);
        Claim claimAtCurrentCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(currentCycleExpectedDeliveryDate, currentCycleChildrenDobs);

        //Current payment cycle
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claimAtCurrentCycle);

        //Previous payment cycle
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claimAtPreviousCycle);

        return aDetermineEntitlementMessageContext(
                currentPaymentCycle,
                previousPaymentCycle,
                claimAtCurrentCycle);
    }

    //Argument order is: previousCycleExpectedDeliveryDate, previousCycleChildrenDobs, currentCycleChildrenDobs
    private static Stream<Arguments> provideArgumentsForClaimantBecomingPregnantWithChildrenInCurrentCycle() {
        return Stream.of(
                Arguments.of(TWO_CHILDREN, NOT_PREGNANT, NOT_PREGNANT),
                Arguments.of(TWO_CHILDREN, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS),
                Arguments.of(NO_CHILDREN, NOT_PREGNANT, NOT_PREGNANT)
        );
    }

    //Argument order is: qualifyingBenefitStatus, previousCycleChildrenDobs
    private static Stream<Arguments> provideArgumentsForQualifyingBenefitStatusAndPreviousChildrenDob() {
        return Stream.of(
                Arguments.of(NOT_CONFIRMED, NO_CHILDREN),
                Arguments.of(CONFIRMED, NO_CHILDREN),
                Arguments.of(NOT_CONFIRMED, SINGLE_THREE_YEAR_OLD),
                Arguments.of(CONFIRMED, SINGLE_THREE_YEAR_OLD)
        );
    }

    private static Stream<Arguments> provideArgumentsForChildrenInPreviousCycle() {
        return Stream.of(
                Arguments.of(TWO_CHILDREN),
                Arguments.of(NO_CHILDREN)
        );
    }

}
