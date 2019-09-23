package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessor;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.atLeastOnce;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageProcessorSchedulerTest {

    @Mock
    MessageProcessor processor;

    @InjectMocks
    MessageProcessorScheduler scheduler;

    @Test
    void shouldInvokeMessageProcessorForEveryMessageType() {
        invokeAllScheduledMethods();

        assertAllMessageTypesProcessed();
    }

    private void invokeAllScheduledMethods() {
        Method[] methods = scheduler.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getAnnotation(Scheduled.class) != null) {
                try {
                    method.invoke(scheduler);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    fail(e);
                }
            }
        }
    }

    private void assertAllMessageTypesProcessed() {
        ArgumentCaptor<MessageType> argumentCaptor = ArgumentCaptor.forClass(MessageType.class);
        verify(processor, atLeastOnce()).processMessagesOfType(argumentCaptor.capture());
        List<MessageType> scheduledMessageTypes = argumentCaptor.getAllValues();
        assertThat(scheduledMessageTypes).containsExactlyInAnyOrder(MessageType.values());
    }
}