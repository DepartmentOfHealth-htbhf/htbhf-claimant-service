package uk.gov.dhsc.htbhf.claimant.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.dhsc.htbhf.claimant.requestcontext.RequestContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ErrorHandler extends ResponseEntityExceptionHandler {

    public static final String VALIDATION_ERROR_MESSAGE = "There were validation issues with the request.";
    public static final String UNREADABLE_ERROR_MESSAGE = "The request could not be parsed.";

    private final RequestContext requestContext;

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                               HttpHeaders headers,
                                                               HttpStatus status,
                                                               WebRequest request) {

        ErrorResponse errorResponse = convertAndLogBindingResult(exception.getBindingResult());
        return handleExceptionInternal(exception, errorResponse, headers, BAD_REQUEST, request);
    }

    @Override
    public ResponseEntity<Object> handleBindException(BindException exception, HttpHeaders headers, HttpStatus status, WebRequest request) {
        ErrorResponse errorResponse = convertAndLogBindingResult(exception.getBindingResult());
        return handleExceptionInternal(exception, errorResponse, headers, BAD_REQUEST, request);
    }

    private ErrorResponse convertAndLogBindingResult(BindingResult bindingResult) {
        final List<ErrorResponse.FieldError> errors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.add(ErrorResponse.FieldError.builder()
                        .message(error.getDefaultMessage())
                        .field(error.getField())
                        .build()));
        bindingResult.getGlobalErrors().forEach(error ->
                errors.add(ErrorResponse.FieldError.builder()
                        .message(error.getDefaultMessage())
                        .field(error.getObjectName())
                        .build()));

        log.warn("Binding error(s) during {} request to {}: {}", requestContext.getMethod(), requestContext.getServletPath(), errors);

        return ErrorResponse.builder()
                .fieldErrors(errors)
                .requestId(requestContext.getRequestId())
                .status(BAD_REQUEST.value())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .message(VALIDATION_ERROR_MESSAGE)
                .build();
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException cause = (InvalidFormatException) ex.getCause();
            String path = cause.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
            ErrorResponse.FieldError fieldError = ErrorResponse.FieldError.builder()
                    .message(String.format("'%s' could not be parsed as a %s", cause.getValue(), cause.getTargetType().getSimpleName()))
                    .field(path)
                    .build();
            ErrorResponse response = ErrorResponse.builder()
                    .fieldErrors(Collections.singletonList(fieldError))
                    .requestId(requestContext.getRequestId())
                    .status(BAD_REQUEST.value())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .message(UNREADABLE_ERROR_MESSAGE)
                    .build();
            return handleExceptionInternal(ex, response, headers, BAD_REQUEST, request);
        }
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleOthers(Exception exception, WebRequest request) {
        log.error("An error occurred during {} request to {}:", requestContext.getMethod(), requestContext.getServletPath(), exception);

        ErrorResponse body = ErrorResponse.builder()
                .requestId(requestContext.getRequestId())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .status(INTERNAL_SERVER_ERROR.value())
                .message("An internal server error occurred")
                .build();

        return handleExceptionInternal(exception, body, new HttpHeaders(), INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        Object response = body;
        if (response == null) {
            log.warn("Handling {} during {} request to {}: {}",
                    ex.getClass().getSimpleName(), requestContext.getMethod(), requestContext.getServletPath(), ex.getMessage());
            response = ErrorResponse.builder()
                    .requestId(requestContext.getRequestId())
                    .status(status.value())
                    .message(status.getReasonPhrase())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .build();
        }
        return super.handleExceptionInternal(ex, response, headers, status, request);
    }
}
