package uk.gov.dhsc.htbhf.claimant.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.WebRequest;
import uk.gov.dhsc.htbhf.claimant.requestcontext.RequestContext;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {

    private static final String REQUEST_ID = "myRequestId";
    private static final String ERROR_MESSAGE_1 = "error1";
    private static final String ERROR_MESSAGE_2 = "error2";

    @Mock
    BindingResult bindingResult;
    @Mock
    HttpHeaders httpHeaders;
    @Mock
    WebRequest webRequest;
    @Mock
    RequestContext requestContext;

    @InjectMocks
    ErrorHandler handler;

    @Test
    void shouldExtractFieldErrorsFromBindException() {
        // Given
        FieldError fieldError1 = new FieldError("objectName", "field1", ERROR_MESSAGE_1);
        FieldError fieldError2 = new FieldError("objectName", "field2", ERROR_MESSAGE_2);
        given(bindingResult.getFieldErrors()).willReturn(asList(fieldError1, fieldError2));
        given(requestContext.getRequestId()).willReturn(REQUEST_ID);

        // When
        ResponseEntity<Object> responseEntity = handler.handleBindException(new BindException(bindingResult), httpHeaders, HttpStatus.BAD_REQUEST, webRequest);

        // Then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();
        assertThat(errorResponse.getRequestId()).isEqualTo(REQUEST_ID);
        List<ErrorResponse.FieldError> fieldErrors = errorResponse.getFieldErrors();
        assertThat(fieldErrors.size()).isEqualTo(2);
        assertThat(fieldErrors.get(0).getField()).isEqualTo("field1");
        assertThat(fieldErrors.get(0).getMessage()).isEqualTo(ERROR_MESSAGE_1);
        assertThat(fieldErrors.get(1).getField()).isEqualTo("field2");
        assertThat(fieldErrors.get(1).getMessage()).isEqualTo(ERROR_MESSAGE_2);
    }

    @Test
    void shouldExtractGlobalErrorsFromBindException() {
        // Given
        ObjectError globalError1 = new ObjectError("object1", ERROR_MESSAGE_1);
        ObjectError globalError2 = new ObjectError("object2", ERROR_MESSAGE_2);
        given(bindingResult.getGlobalErrors()).willReturn(asList(globalError1, globalError2));
        given(requestContext.getRequestId()).willReturn(REQUEST_ID);

        // When
        ResponseEntity<Object> responseEntity = handler.handleBindException(new BindException(bindingResult), httpHeaders, HttpStatus.BAD_REQUEST, webRequest);

        // Then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();
        assertThat(errorResponse.getRequestId()).isEqualTo(REQUEST_ID);
        List<ErrorResponse.FieldError> fieldErrors = errorResponse.getFieldErrors();
        assertThat(fieldErrors.size()).isEqualTo(2);
        assertThat(fieldErrors.get(0).getField()).isEqualTo("object1");
        assertThat(fieldErrors.get(0).getMessage()).isEqualTo(ERROR_MESSAGE_1);
        assertThat(fieldErrors.get(1).getField()).isEqualTo("object2");
        assertThat(fieldErrors.get(1).getMessage()).isEqualTo(ERROR_MESSAGE_2);
    }

}