package uk.gov.dhsc.htbhf.claimant.requestcontext;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * A component wrapper around the MDC that supports non-static access to MDC methods.
 */
@Component
public class MDCWrapper {

    public void put(String key, String value) {
        MDC.put(key, value);
    }

    public void remove(String key) {
        MDC.remove(key);
    }
}
