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

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCycleSchedulerTest {

    private static final int END_DATE_OFFSET_DAYS = 2;

    @Mock
    PaymentCycleRepository paymentCycleRepository;

    @Mock
    CreateNewPaymentCycleJob job;

    PaymentCycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentCycleScheduler(paymentCycleRepository, job, END_DATE_OFFSET_DAYS);
    }

    @Test
    void shouldInvokeJobOnceForEachCycleWithIncrementedStartDate() {
        LocalDate today = LocalDate.now();
        List<ClosingPaymentCycle> matchingCycles = List.of(
                createClosingPaymentCycle(randomUUID(), randomUUID(), today),
                createClosingPaymentCycle(randomUUID(), randomUUID(), today.minusDays(2)),
                createClosingPaymentCycle(randomUUID(), randomUUID(), today.minusDays(1))
        );
        given(paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(any())).willReturn(matchingCycles);

        scheduler.createNewPaymentCycles();

        verify(paymentCycleRepository).findActiveClaimsWithCycleEndingOnOrBefore(today.plusDays(END_DATE_OFFSET_DAYS));
        matchingCycles.forEach(match -> {
            verify(job).createNewPaymentCycle(match.getClaimId(), match.getCycleId(), match.getCycleEndDate().plusDays(1));
        });
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
                Timestamp timeStamp = new Timestamp(date.getTime());
                return timeStamp;
            }
        };
    }
}