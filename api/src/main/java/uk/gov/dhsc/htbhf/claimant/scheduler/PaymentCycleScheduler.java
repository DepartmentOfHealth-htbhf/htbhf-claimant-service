package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.repository.ClosingPaymentCycle;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.requestcontext.aop.NewRequestContextWithSessionId;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.errorhandler.ExceptionDetailGenerator.constructExceptionDetail;

/**
 * Responsible for creating new {@link uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle} objects
 * for active claims whose current PaymentCycle is due to come to an end.
 * This scheduler simply invokes the CreateNewPaymentCycleJob for each such claim.
 */
@Slf4j
@Component
public class PaymentCycleScheduler {

    private final PaymentCycleRepository paymentCycleRepository;
    private final CreateNewPaymentCycleJob job;
    private final int endDateOffsetDays;

    public PaymentCycleScheduler(
            PaymentCycleRepository paymentCycleRepository,
            CreateNewPaymentCycleJob job,
            @Value("${payment-cycle.schedule.end-date-offset-days:0}") int endDateOffsetDays) {

        this.paymentCycleRepository = paymentCycleRepository;
        this.job = job;
        this.endDateOffsetDays = endDateOffsetDays;
    }

    @Scheduled(cron = "${payment-cycle.schedule.cron-schedule}")
    @SchedulerLock(
            name = "Create new payment cycles",
            lockAtLeastForString = "${payment-cycle.schedule.minimum-lock-time}",
            lockAtMostForString = "${payment-cycle.schedule.maximum-lock-time}")
    @NewRequestContextWithSessionId(sessionId = "PaymentCycleScheduler")
    public void createNewPaymentCycles() {
        LocalDate cycleEndDate = LocalDate.now().plusDays(endDateOffsetDays);
        log.debug("Querying for active claims with cycles ending on {}", cycleEndDate);
        List<ClosingPaymentCycle> cycles = paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(cycleEndDate);

        log.info("Creating new PaymentCycles for {} claims", cycles.size());
        cycles.forEach(this::createNewPaymentCycle);
        log.debug("Finished creating new PaymentCycles for {} claims", cycles.size());
    }

    private void createNewPaymentCycle(ClosingPaymentCycle cycle) {
        try {
            job.createNewPaymentCycle(cycle.getClaimId(), cycle.getCycleId(), cycle.getCycleEndDate().plusDays(1));
        } catch (RuntimeException e) {
            log.error("Unable to create new payment cycle for claim {}: {}", cycle.getClaimId(), constructExceptionDetail(e), e);
        }
    }
}
