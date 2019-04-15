package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.requestcontext.RequestIdFilter.REQUEST_ID_HEADER;
import static uk.gov.dhsc.htbhf.requestcontext.RequestIdFilter.SESSION_ID_HEADER;

@ExtendWith(MockitoExtension.class)
class SchedulerHeaderInterceptorTest {

    private final SchedulerHeaderInterceptor interceptor = new SchedulerHeaderInterceptor();

    @Test
    void shouldAddRequestIdAndSessionIdToHeaders() throws IOException {
        var execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(new MockClientHttpRequest(), new byte[]{}, execution);

        ArgumentCaptor<HttpRequest> argumentCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(execution).execute(argumentCaptor.capture(), any());
        HttpRequest request = argumentCaptor.getValue();
        assertThat(request.getHeaders().get(REQUEST_ID_HEADER)).isNotNull();
        assertThat(request.getHeaders().get(SESSION_ID_HEADER)).isNotNull();
    }
}
