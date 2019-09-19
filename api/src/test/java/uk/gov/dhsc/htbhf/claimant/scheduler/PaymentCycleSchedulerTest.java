package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.repository.ClosingPaymentCycle;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class PaymentCycleSchedulerTest {

    private static final int END_DATE_OFFSET_DAYS = 2;
    private static final LocalDate TODAY = LocalDate.now();

    @Mock
    PaymentCycleRepository paymentCycleRepository;

    @Mock
    CreateNewPaymentCycleJob job;

    private PaymentCycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentCycleScheduler(paymentCycleRepository, job, END_DATE_OFFSET_DAYS);
    }

    @Test
    void shouldContinueToProcessAfterException() {
        ClosingPaymentCycle paymentCycle1 = createClosingPaymentCycle(randomUUID(), randomUUID(), TODAY);
        ClosingPaymentCycle paymentCycle2 = createClosingPaymentCycle(randomUUID(), randomUUID(), TODAY.minusDays(2));
        ClosingPaymentCycle paymentCycle3 = createClosingPaymentCycle(randomUUID(), randomUUID(), TODAY.minusDays(1));
        List<ClosingPaymentCycle> matchingCycles = List.of(paymentCycle1, paymentCycle2, paymentCycle3);

        given(paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(any())).willReturn(matchingCycles);
        given(job.createNewPaymentCycle(any(), any(), any()))
                .willThrow(new RuntimeException("foo"))
                .willReturn(aValidPaymentCycle())
                .willReturn(aValidPaymentCycle());

        scheduler.createNewPaymentCycles();

        verify(paymentCycleRepository).findActiveClaimsWithCycleEndingOnOrBefore(TODAY.plusDays(END_DATE_OFFSET_DAYS));
        verify(job).createNewPaymentCycle(paymentCycle1.getClaimId(), paymentCycle1.getCycleId(), paymentCycle1.getCycleEndDate().plusDays(1));
        verify(job).createNewPaymentCycle(paymentCycle2.getClaimId(), paymentCycle2.getCycleId(), paymentCycle2.getCycleEndDate().plusDays(1));
        verify(job).createNewPaymentCycle(paymentCycle3.getClaimId(), paymentCycle3.getCycleId(), paymentCycle3.getCycleEndDate().plusDays(1));
        verifyNoMoreInteractions(job);
    }

    @Test
    void shouldInvokeJobOnceForEachCycleWithIncrementedStartDate() {
        ClosingPaymentCycle paymentCycle1 = createClosingPaymentCycle(randomUUID(), randomUUID(), TODAY);
        ClosingPaymentCycle paymentCycle2 = createClosingPaymentCycle(randomUUID(), randomUUID(), TODAY.minusDays(2));
        ClosingPaymentCycle paymentCycle3 = createClosingPaymentCycle(randomUUID(), randomUUID(), TODAY.minusDays(1));
        List<ClosingPaymentCycle> matchingCycles = List.of(paymentCycle1, paymentCycle2, paymentCycle3);

        given(paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(any())).willReturn(matchingCycles);

        scheduler.createNewPaymentCycles();

        verify(paymentCycleRepository).findActiveClaimsWithCycleEndingOnOrBefore(TODAY.plusDays(END_DATE_OFFSET_DAYS));
        verify(job).createNewPaymentCycle(paymentCycle1.getClaimId(), paymentCycle1.getCycleId(), paymentCycle1.getCycleEndDate().plusDays(1));
        verify(job).createNewPaymentCycle(paymentCycle2.getClaimId(), paymentCycle2.getCycleId(), paymentCycle2.getCycleEndDate().plusDays(1));
        verify(job).createNewPaymentCycle(paymentCycle3.getClaimId(), paymentCycle3.getCycleId(), paymentCycle3.getCycleEndDate().plusDays(1));
        verifyNoMoreInteractions(job);
    }

    @Test
    void shouldQueryForCyclesEndingYesterdayWhenOffsetIsMinus1() {
        this.scheduler = new PaymentCycleScheduler(paymentCycleRepository, job, -1);
        given(paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(any())).willReturn(emptyList());

        scheduler.createNewPaymentCycles();

        verify(paymentCycleRepository).findActiveClaimsWithCycleEndingOnOrBefore(LocalDate.now().minusDays(1));
    }

    ClosingPaymentCycle createClosingPaymentCycle(UUID claimId, UUID cycleId, LocalDate endDate) {
        return new ClosingPaymentCycle() {
            @Override
            public String getClaimIdString() {
                return claimId.toString();
            }

            @Override
            public String getCycleIdString() {
                return cycleId.toString();
            }

            @Override
            public Timestamp getCycleEndDateTimestamp() {
                Date date = Date.from(endDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
                return new Timestamp(date.getTime());
            }
        };
    }
}
