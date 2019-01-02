package uk.gov.dhsc.htbhf.claimant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;
import uk.gov.dhsc.htbhf.claimant.requestcontext.RequestContext;

import java.util.Map;

/**
 * Responsible for adding custom properties to error responses, e.g. the id of the request.
 */
@Component
public class ErrorAttributes  extends DefaultErrorAttributes {

    @Autowired
    private RequestContext requestContext;

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest request, boolean includeStackTrace) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, includeStackTrace);

        errorAttributes.put("requestid", requestContext.getRequestId());

        return errorAttributes;
    }
}
