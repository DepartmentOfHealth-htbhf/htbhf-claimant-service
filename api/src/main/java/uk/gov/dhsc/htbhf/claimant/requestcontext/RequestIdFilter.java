package uk.gov.dhsc.htbhf.claimant.requestcontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.util.StringUtils.isEmpty;

@Component
@RequiredArgsConstructor
public class RequestIdFilter extends OncePerRequestFilter {

    /**
     * The name of the request ID in the MDC context. Must match the name in logback configuration.
     */
    public static final String REQUEST_ID_MDC_KEY = "request.id";
    /**
     * The name of the request id header.
     */
    public static final String REQUEST_ID_HEADER = "X-REQUEST-ID";

    /**
     * The name of the session ID in the MDC context. Must match the name in logback configuration.
     */
    public static final String SESSION_ID_MDC_KEY = "session.id";
    /**
     * The name of the session id header.
     */
    public static final String SESSION_ID_HEADER = "X-SESSION-ID";

    private final RequestContext requestContext;
    private final MDCWrapper mdcWrapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (isEmpty(requestId)) {
                requestId = UUID.randomUUID().toString();
            }
            mdcWrapper.put(REQUEST_ID_MDC_KEY, requestId);
            requestContext.setRequestId(requestId);

            String sessionId = request.getHeader(SESSION_ID_HEADER);
            mdcWrapper.put(SESSION_ID_MDC_KEY, sessionId);
            requestContext.setSessionId(sessionId);

            requestContext.setMethod(request.getMethod());
            requestContext.setServletPath(request.getServletPath());

            filterChain.doFilter(request, response);
        } finally {
            mdcWrapper.remove(REQUEST_ID_MDC_KEY);
            mdcWrapper.remove(SESSION_ID_MDC_KEY);
        }

    }
}
