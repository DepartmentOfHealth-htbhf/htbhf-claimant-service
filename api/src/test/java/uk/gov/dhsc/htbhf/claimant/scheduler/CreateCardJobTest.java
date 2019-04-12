package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateCardJobTest {

    @Mock
    private NewCardScheduleService newCardScheduleService;

    @InjectMocks
    private CreateCardJob createCardJob;

    @Test
    void shouldCallService() {
        createCardJob.executeInternal(mock(JobExecutionContext.class));

        verify(newCardScheduleService).createNewCards();
    }
}
