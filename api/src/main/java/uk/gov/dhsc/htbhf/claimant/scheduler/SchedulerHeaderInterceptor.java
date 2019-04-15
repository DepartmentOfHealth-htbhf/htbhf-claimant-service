package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.requestcontext.RequestIdFilter.REQUEST_ID_HEADER;
import static uk.gov.dhsc.htbhf.requestcontext.RequestIdFilter.SESSION_ID_HEADER;

/**
 * Creates an http interceptor used to be used by the scheduler. The interceptor
 * creates random request and session ids and appends them to the incoming request headers.
 * This is required as {@link uk.gov.dhsc.htbhf.requestcontext.HeaderInterceptor} only works in
 * the scope of a web request.
 */
public class SchedulerHeaderInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        var headers = request.getHeaders();
        headers.add(REQUEST_ID_HEADER, UUID.randomUUID().toString());
        headers.add(SESSION_ID_HEADER, UUID.randomUUID().toString());
        return execution.execute(request, body);
    }
}
