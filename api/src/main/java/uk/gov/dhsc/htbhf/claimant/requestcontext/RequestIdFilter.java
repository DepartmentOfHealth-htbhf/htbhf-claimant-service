package uk.gov.dhsc.htbhf.claimant.requestcontext;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    /** The name of the request ID in the MDC context. Must match the name in logback configuration. */
    private static final String MDC_KEY = "request.id";
    /** The name of the request id header. */
    private static final String REQUEST_ID_HEADER = "X-REQUEST-ID";

    @Autowired
    private RequestContext requestContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token;
            if (StringUtils.isEmpty(request.getHeader(REQUEST_ID_HEADER))) {
                token = UUID.randomUUID().toString();
            } else {
                token = request.getHeader(REQUEST_ID_HEADER);
            }
            MDC.put(MDC_KEY, token);
            requestContext.setRequestId(token);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }

    }
}
