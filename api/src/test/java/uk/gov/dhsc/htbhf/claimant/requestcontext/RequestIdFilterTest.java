package uk.gov.dhsc.htbhf.claimant.requestcontext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static uk.gov.dhsc.htbhf.claimant.requestcontext.RequestIdFilter.REQUEST_ID_HEADER;
import static uk.gov.dhsc.htbhf.claimant.requestcontext.RequestIdFilter.REQUEST_ID_MDC_KEY;
import static uk.gov.dhsc.htbhf.claimant.requestcontext.RequestIdFilter.SESSION_ID_HEADER;
import static uk.gov.dhsc.htbhf.claimant.requestcontext.RequestIdFilter.SESSION_ID_MDC_KEY;

@ExtendWith(MockitoExtension.class)
class RequestIdFilterTest {

    @Mock
    RequestContext requestContext;
    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain filterChain;
    @Mock
    MDCWrapper mdcWrapper;

    @InjectMocks
    RequestIdFilter filter;

    @Test
    void shouldAssignRequestIdToMDCAndRequestContext() throws Exception {
        // Given
        String requestId = "MyRequestId";
        given(request.getHeader(REQUEST_ID_HEADER)).willReturn(requestId);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        InOrder inOrder = inOrder(mdcWrapper, requestContext, filterChain);

        inOrder.verify(mdcWrapper).put(REQUEST_ID_MDC_KEY, requestId);
        inOrder.verify(requestContext).setRequestId(requestId);
        inOrder.verify(filterChain).doFilter(request, response);
        inOrder.verify(mdcWrapper).remove(REQUEST_ID_MDC_KEY);
    }

    @Test
    void shouldAssignSessionIdToMDCAndRequestContext() throws Exception {
        // Given
        String sessionId = "MySessionId";
        willReturn("stop mockito reporting strict stubbing argument mismatch.").given(request).getHeader(anyString());
        willReturn(sessionId).given(request).getHeader(SESSION_ID_HEADER);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        InOrder inOrder = inOrder(mdcWrapper, requestContext, filterChain);

        inOrder.verify(mdcWrapper).put(SESSION_ID_MDC_KEY, sessionId);
        inOrder.verify(requestContext).setSessionId(sessionId);
        inOrder.verify(filterChain).doFilter(request, response);
        inOrder.verify(mdcWrapper).remove(SESSION_ID_MDC_KEY);
    }

    @Test
    void shouldCreateRequestIdWhenNoneProvided() throws Exception {
        // Given

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        InOrder inOrder = inOrder(mdcWrapper, requestContext, filterChain);

        inOrder.verify(mdcWrapper).put(eq(REQUEST_ID_MDC_KEY), anyString());
        inOrder.verify(requestContext).setRequestId(anyString());
        inOrder.verify(filterChain).doFilter(request, response);
        inOrder.verify(mdcWrapper).remove(REQUEST_ID_MDC_KEY);
    }

    @Test
    void shouldPopulateMethodAndServletPathOfRequestContext() throws Exception {
        // Given
        String method = "POST";
        given(request.getMethod()).willReturn(method);
        String servletPath = "/my/path";
        given(request.getServletPath()).willReturn(servletPath);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then

        verify(requestContext).setMethod(method);
        verify(requestContext).setServletPath(servletPath);
    }

    @Test
    void shouldClearMDCAfterException() throws Exception {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected Error");
        willThrow(exception).given(filterChain).doFilter(request, response);

        // When
        Throwable thrown = catchThrowable(() -> filter.doFilterInternal(request, response, filterChain));

        // Then
        assertThat(thrown).isEqualTo(exception);

        InOrder inOrder = inOrder(mdcWrapper, requestContext, filterChain);
        inOrder.verify(mdcWrapper).put(eq(REQUEST_ID_MDC_KEY), anyString());
        inOrder.verify(requestContext).setRequestId(anyString());
        inOrder.verify(filterChain).doFilter(request, response);
        inOrder.verify(mdcWrapper).remove(REQUEST_ID_MDC_KEY);
    }

}