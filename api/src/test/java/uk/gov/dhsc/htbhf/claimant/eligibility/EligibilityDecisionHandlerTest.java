package uk.gov.dhsc.htbhf.claimant.eligibility;

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
import uk.gov.dhsc.htbhf.claimant.entity.CardStatus;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.dwp.model.v2.EligibilityOutcome;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.TestConstants.ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR;
import static uk.gov.dhsc.htbhf.TestConstants.SINGLE_NEARLY_FOUR_YEAR_OLD;
import static uk.gov.dhsc.htbhf.TestConstants.SINGLE_THREE_YEAR_OLD;
import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.PENDING_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_ACTIVE_TO_EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_PENDING_EXPIRY_TO_EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimStatusAndCardStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDateAndChildrenDobs;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartDateAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NOT_PREGNANT;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

@ExtendWith(MockitoExtension.class)
class EligibilityDecisionHandlerTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private DetermineEntitlementNotificationHandler determineEntitlementNotificationHandler;
    @Mock
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;
    @Mock
    private EventAuditor eventAuditor;
    @Mock
    private ClaimMessageSender claimMessageSender;
    @Mock
    private MessageQueueClient messageQueueClient;

    @InjectMocks
    private EligibilityDecisionHandler handler;

    @Test
    void shouldMakePaymentAndSendPaymentEmailForClaim() {
        // Given
        Claim claim = aValidClaim();
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claim);
        given(pregnancyEntitlementCalculator.currentCycleIsSecondToLastCycleWithPregnancyVouchers(any())).willReturn(false);

        // When
        handler.handleEligibleDecision(claim, currentPaymentCycle);

        // Then
        MessagePayload expectedPayload = MessagePayloadFactory.buildMakePaymentMessagePayload(currentPaymentCycle);
        verify(messageQueueClient).sendMessage(expectedPayload, MessageType.MAKE_PAYMENT);
        verify(pregnancyEntitlementCalculator).currentCycleIsSecondToLastCycleWithPregnancyVouchers(currentPaymentCycle);
    }

    @Test
    void shouldMoveAPendingExpiryClaimToActiveWhenTheClaimantIsEligible() {
        //Given
        Claim claim = aClaimWithClaimStatusAndCardStatus(PENDING_EXPIRY, PENDING_CANCELLATION);
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claim);
        // current payment cycle is not second to last one with vouchers
        given(pregnancyEntitlementCalculator.currentCycleIsSecondToLastCycleWithPregnancyVouchers(any())).willReturn(false);

        //When
        handler.handleEligibleDecision(claim, currentPaymentCycle);

        // Then
        verifyClaimSavedWithStatus(ACTIVE, CardStatus.ACTIVE);
        MessagePayload expectedPayload = MessagePayloadFactory.buildMakePaymentMessagePayloadForRestartedPayment(currentPaymentCycle);
        verify(messageQueueClient).sendMessage(expectedPayload, MessageType.MAKE_PAYMENT);
        verify(pregnancyEntitlementCalculator).currentCycleIsSecondToLastCycleWithPregnancyVouchers(currentPaymentCycle);
    }

    @Test
    void shouldSendReportABirthReminderEmailWhenClaimantReceivesSecondToLastPregnancyVouchers() {
        // Given
        Claim claim = aValidClaim();
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claim);
        // current payment cycle is second to last one with vouchers
        given(pregnancyEntitlementCalculator.currentCycleIsSecondToLastCycleWithPregnancyVouchers(any())).willReturn(true);

        // When
        handler.handleEligibleDecision(claim, currentPaymentCycle);

        // Then
        verify(pregnancyEntitlementCalculator).currentCycleIsSecondToLastCycleWithPregnancyVouchers(currentPaymentCycle);
        verify(claimMessageSender).sendReportABirthEmailMessage(claim);
    }

    //Test for HTBHF-2182 has the following context:
    // Previous cycle: children exist and are under 4 but will be 4 in the next cycle, not pregnant
    // Current cycle: no children and not pregnant
    @ParameterizedTest(name = "Qualifying benefit status={0}")
    @ValueSource(strings = {"CONFIRMED", "NOT_CONFIRMED"})
    void shouldExpireClaimWhenIneligibleWithNoChildrenAndNotPregnant(EligibilityOutcome eligibilityOutcome) {
        //Given
        //Claimant has children under 4 at the previous payment cycle that would still over 4 now (but not reported by DWP).
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(true);
        given(childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(any(), any())).willReturn(false);

        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;

        Claim claimAtPreviousCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(NOT_PREGNANT, SINGLE_NEARLY_FOUR_YEAR_OLD);
        Claim claimAtCurrentCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(NOT_PREGNANT, currentCycleChildrenDobs);
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claimAtCurrentCycle);
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claimAtPreviousCycle);

        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, eligibilityOutcome, currentCycleChildrenDobs);
        given(pregnancyEntitlementCalculator.claimantIsPregnantInCycle(currentPaymentCycle)).willReturn(false);

        //When
        handler.handleIneligibleDecisionForActiveClaim(claimAtCurrentCycle, previousPaymentCycle, currentPaymentCycle, decision);

        //Then
        verifyClaimSavedWithStatus(ClaimStatus.EXPIRED, PENDING_CANCELLATION);
        LocalDate currentPaymentCycleStartDate = currentPaymentCycle.getCycleStartDate();
        verify(pregnancyEntitlementCalculator).claimantIsPregnantInCycle(currentPaymentCycle);
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(previousPaymentCycle);
        verify(childDateOfBirthCalculator).hadChildrenUnderFourAtGivenDate(SINGLE_NEARLY_FOUR_YEAR_OLD, currentPaymentCycleStartDate);
        verify(eventAuditor).auditExpiredClaim(claimAtCurrentCycle);
        verify(claimMessageSender).sendReportClaimMessage(claimAtCurrentCycle, UPDATED_FROM_ACTIVE_TO_EXPIRED);
        verifyNoMoreInteractions(determineEntitlementNotificationHandler);
    }

    //Test for HTBHF-2185 has the following context:
    // Previous cycle: children exist and are under 4, not pregnant or pregnant (parameterised)
    // Current cycle: no children and not pregnant
    @ParameterizedTest(name = "Expected delivery date previous cycle={0}")
    @MethodSource("provideArgumentsForPreviousCycleExpectedDeliveryDate")
    void shouldExpireClaimWhenChildDisappearsFromFeedAndNotPregnant(LocalDate previousCycleExpectedDeliveryDate) {
        //Given
        //Claimant has children under 4 at the previous payment cycle that would still be under 4 now (but not reported by DWP).
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(true);
        given(childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(any(), any())).willReturn(true);

        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        Claim claimAtPreviousCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(previousCycleExpectedDeliveryDate, SINGLE_THREE_YEAR_OLD);
        Claim claimAtCurrentCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(NOT_PREGNANT, currentCycleChildrenDobs);
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claimAtCurrentCycle);
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claimAtPreviousCycle);

        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, EligibilityOutcome.CONFIRMED, currentCycleChildrenDobs);
        given(pregnancyEntitlementCalculator.claimantIsPregnantInCycle(currentPaymentCycle)).willReturn(false);

        //When
        handler.handleIneligibleDecisionForActiveClaim(claimAtCurrentCycle, previousPaymentCycle, currentPaymentCycle, decision);

        //Then
        verifyClaimSavedWithStatus(ClaimStatus.EXPIRED, PENDING_CANCELLATION);
        verify(determineEntitlementNotificationHandler).sendNoChildrenOnFeedClaimNoLongerEligibleEmail(claimAtCurrentCycle);
        LocalDate currentPaymentCycleStartDate = currentPaymentCycle.getCycleStartDate();
        verify(pregnancyEntitlementCalculator).claimantIsPregnantInCycle(currentPaymentCycle);
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(previousPaymentCycle);
        verify(childDateOfBirthCalculator).hadChildrenUnderFourAtGivenDate(SINGLE_THREE_YEAR_OLD, currentPaymentCycleStartDate);
        verify(eventAuditor).auditExpiredClaim(claimAtCurrentCycle);
        verify(claimMessageSender).sendReportClaimMessage(claimAtCurrentCycle, UPDATED_FROM_ACTIVE_TO_EXPIRED);
    }

    //HTBHF-1757 Children in current cycle, DWP returns ineligible, varying on pregnancy and children in previous cycle
    @ParameterizedTest(name = "Children DOB previous cycle={0}, expected delivery date previous cycle={1}, expected delivery date current cycle={2}")
    @MethodSource("provideArgumentsForPendingExpiryTestsWithChildrenInCurrentCycle")
    void shouldUpdateClaimToPendingExpiryWhenClaimantIsNotEligibleWithChildrenInCurrentCycle(
            List<LocalDate> previousCycleChildrenDobs,
            LocalDate previousCycleExpectedDeliveryDate,
            LocalDate currentCycleExpectedDeliveryDate) {

        //Had children in last cycle who are under 4 at the start of the current payment cycle.
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(true);
        given(childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(any(), any())).willReturn(true);

        //Given - their children would still be under 4 but DWP doesn't return them if they're NOT_CONFIRMED.
        Claim claimAtPreviousCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(previousCycleExpectedDeliveryDate, previousCycleChildrenDobs);
        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        Claim claimAtCurrentCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(currentCycleExpectedDeliveryDate, currentCycleChildrenDobs);
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claimAtCurrentCycle);
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claimAtPreviousCycle);

        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, EligibilityOutcome.NOT_CONFIRMED, currentCycleChildrenDobs);
        given(pregnancyEntitlementCalculator.claimantIsPregnantInCycle(currentPaymentCycle)).willReturn(false);

        //When
        handler.handleIneligibleDecisionForActiveClaim(claimAtCurrentCycle, previousPaymentCycle, currentPaymentCycle, decision);

        //Then
        verifyClaimSavedWithStatus(ClaimStatus.PENDING_EXPIRY, PENDING_CANCELLATION);
        verify(determineEntitlementNotificationHandler).sendClaimNoLongerEligibleEmail(claimAtCurrentCycle);
        LocalDate currentCycleStartDate = currentPaymentCycle.getCycleStartDate();
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(previousPaymentCycle);
        verify(childDateOfBirthCalculator).hadChildrenUnderFourAtGivenDate(ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR, currentCycleStartDate);
        verify(pregnancyEntitlementCalculator).claimantIsPregnantInCycle(currentPaymentCycle);
        verify(claimMessageSender).sendReportClaimMessage(claimAtCurrentCycle, UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY);
        verifyNoMoreInteractions(childDateOfBirthCalculator, eventAuditor);
    }

    //HTBHF-1757 No children in current cycle, are pregnant but ineligible from DWP
    @ParameterizedTest(name = "Children DOB in previous cycle={0}")
    @MethodSource("provideArgumentsForChildrenInPreviousCycle")
    void shouldUpdateClaimToPendingExpiryWhenClaimantIsNotEligibleButStillPregnant(List<LocalDate> previousCycleChildrenDobs) {
        //Given
        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        Claim claimAtPreviousCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, previousCycleChildrenDobs);
        Claim claimAtCurrentCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, currentCycleChildrenDobs);
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claimAtCurrentCycle);
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claimAtPreviousCycle);

        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, EligibilityOutcome.NOT_CONFIRMED, currentCycleChildrenDobs);
        given(pregnancyEntitlementCalculator.claimantIsPregnantInCycle(currentPaymentCycle)).willReturn(true);

        //When
        handler.handleIneligibleDecisionForActiveClaim(claimAtCurrentCycle, previousPaymentCycle, currentPaymentCycle, decision);

        //Then
        verifyClaimSavedWithStatus(ClaimStatus.PENDING_EXPIRY, PENDING_CANCELLATION);
        verify(determineEntitlementNotificationHandler).sendClaimNoLongerEligibleEmail(claimAtCurrentCycle);
        verify(pregnancyEntitlementCalculator).claimantIsPregnantInCycle(currentPaymentCycle);
        verify(claimMessageSender).sendReportClaimMessage(claimAtCurrentCycle, UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY);
        verifyNoMoreInteractions(childDateOfBirthCalculator, eventAuditor);
    }

    @ParameterizedTest(name = "Qualifying benefit status={0}")
    @ValueSource(strings = {"CONFIRMED", "NOT_CONFIRMED"})
    void shouldExpireClaimWhenClaimantWasPregnantWithNoChildrenButNoLongerPregnant(EligibilityOutcome eligibilityOutcome) {
        //Given
        //Had children in last cycle who are under 4 at the start of the current payment cycle.
        given(childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(any())).willReturn(false);

        List<LocalDate> currentCycleChildrenDobs = NO_CHILDREN;
        Claim claimAtPreviousCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, currentCycleChildrenDobs);
        Claim claimAtCurrentCycle = aClaimWithExpectedDeliveryDateAndChildrenDobs(NOT_PREGNANT, currentCycleChildrenDobs);
        PaymentCycle currentPaymentCycle = aPaymentCycleWithClaim(claimAtCurrentCycle);
        LocalDate previousCycleStartDate = LocalDate.now().minusWeeks(4);
        PaymentCycle previousPaymentCycle = aPaymentCycleWithStartDateAndClaim(previousCycleStartDate, claimAtPreviousCycle);

        lenient().when(pregnancyEntitlementCalculator.claimantIsPregnantInCycle(currentPaymentCycle)).thenReturn(false);
        lenient().when(pregnancyEntitlementCalculator.claimantIsPregnantInCycle(previousPaymentCycle)).thenReturn(true);

        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(INELIGIBLE, eligibilityOutcome, currentCycleChildrenDobs);

        //When
        handler.handleIneligibleDecisionForActiveClaim(claimAtCurrentCycle, previousPaymentCycle, currentPaymentCycle, decision);

        //Then
        verifyClaimSavedWithStatus(ClaimStatus.EXPIRED, PENDING_CANCELLATION);
        verify(childDateOfBirthCalculator).hadChildrenUnder4AtStartOfPaymentCycle(previousPaymentCycle);
        InOrder inOrder = inOrder(pregnancyEntitlementCalculator, pregnancyEntitlementCalculator);
        inOrder.verify(pregnancyEntitlementCalculator).claimantIsPregnantInCycle(currentPaymentCycle);
        inOrder.verify(pregnancyEntitlementCalculator).claimantIsPregnantInCycle(previousPaymentCycle);
        verify(eventAuditor).auditExpiredClaim(claimAtCurrentCycle);
    }

    @Test
    void shouldExpirePendingExpiryClaim() {
        // Given
        Claim claim = aClaimWithClaimStatus(PENDING_EXPIRY);

        // When
        handler.expirePendingExpiryClaim(claim);

        // Then
        assertThat(claim.getClaimStatus()).isEqualTo(EXPIRED);
        verify(claimRepository).save(claim);
        verify(claimMessageSender).sendReportClaimMessage(claim, UPDATED_FROM_PENDING_EXPIRY_TO_EXPIRED);
        verify(eventAuditor).auditExpiredClaim(claim);
    }

    //Argument order is: previousCycleChildrenDobs, previousCycleExpectedDeliveryDate, currentCycleExpectedDeliveryDate
    private static Stream<Arguments> provideArgumentsForPendingExpiryTestsWithChildrenInCurrentCycle() {
        return Stream.of(
                Arguments.of(ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR, NOT_PREGNANT, NOT_PREGNANT),
                Arguments.of(ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS)
        );
    }

    private static Stream<Arguments> provideArgumentsForChildrenInPreviousCycle() {
        return Stream.of(
                Arguments.of(ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR),
                Arguments.of(NO_CHILDREN)
        );
    }

    private static Stream<Arguments> provideArgumentsForPreviousCycleExpectedDeliveryDate() {
        return Stream.of(
                Arguments.of(NOT_PREGNANT),
                Arguments.of(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS)
        );
    }

    private void verifyClaimSavedWithStatus(ClaimStatus claimStatus, CardStatus cardStatus) {
        ArgumentCaptor<Claim> argumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(argumentCaptor.capture());
        Claim claim = argumentCaptor.getValue();
        assertThat(claim.getClaimStatus()).isEqualTo(claimStatus);
        assertThat(claim.getCardStatus()).isEqualTo(cardStatus);
    }
}
