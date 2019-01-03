package uk.gov.dhsc.htbhf.claimant.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.dhsc.htbhf.claimant.requestcontext.RequestContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ControllerAdvice
public class ErrorHandler {

    private static final String VALIDATION_ERROR_MESSAGE = "There were validation issues with the request.";
    private final RequestContext requestContext;

    @Autowired
    public ErrorHandler(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /**
     * Handles validation errors and parses them into a an {@link ErrorResponse}.
     * @param exception validation exception
     * @return ErrorResponse object
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .message(error.getDefaultMessage())
                        .field(error.getField())
                .build())
                .collect(Collectors.toList());

        return ErrorResponse.builder()
                .fieldErrors(fieldErrors)
                .requestId(requestContext.getRequestId())
                .status(BAD_REQUEST.value())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .message(VALIDATION_ERROR_MESSAGE)
                .build();
    }
}
