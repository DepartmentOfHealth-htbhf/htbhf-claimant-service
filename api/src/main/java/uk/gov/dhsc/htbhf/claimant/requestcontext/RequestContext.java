package uk.gov.dhsc.htbhf.claimant.requestcontext;

import lombok.Data;

/**
 * A container for request-scoped context, such as the id of the current request.
 */
@Data
public class RequestContext {

    private String requestId;
    private String sessionId;
    private String method;
    private String servletPath;
}
