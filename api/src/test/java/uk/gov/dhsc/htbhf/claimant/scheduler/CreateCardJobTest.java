package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class CreateCardJobTest {

    @MockBean
    private NewCardScheduleService newCardScheduleService;

    @Autowired
    private CreateCardJob createCardJob;

    @Test
    void shouldCallService() {
        createCardJob.executeInternal(mock(JobExecutionContext.class));

        // should be called when the spring context starts, and in the method call above.
        verify(newCardScheduleService, times(2)).createNewCards();
    }
}
