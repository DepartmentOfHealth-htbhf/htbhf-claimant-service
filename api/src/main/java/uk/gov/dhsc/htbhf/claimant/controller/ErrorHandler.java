package uk.gov.dhsc.htbhf.claimant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.dhsc.htbhf.claimant.requestcontext.RequestContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ErrorHandler {

    private static final String VALIDATION_ERROR_MESSAGE = "There were validation issues with the request.";

    private final RequestContext requestContext;

    /**
     * Handles validation errors and parses them into a an {@link ErrorResponse}.
     *
     * @param exception validation exception
     * @return ErrorResponse object
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException exception) {
        log.info("Validation error", exception);
        List<ErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .message(error.getDefaultMessage())
                        .field(error.getField())
                        .build())
                .collect(toList());

        return ErrorResponse.builder()
                .fieldErrors(fieldErrors)
                .requestId(requestContext.getRequestId())
                .status(BAD_REQUEST.value())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .message(VALIDATION_ERROR_MESSAGE)
                .build();
    }

    /**
     * Handles all exceptions not handled by other exception handler methods.
     *
     * @param exception Exception
     * @return ErrorResponse object
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResponse handleError(Exception exception) {
        log.error("An error occurred", exception);

        return ErrorResponse.builder()
                .requestId(requestContext.getRequestId())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .status(INTERNAL_SERVER_ERROR.value())
                .message("An internal server error occurred")
                .build();
    }

    /**
     * Handles invalid request messages which are unable to be read.
     *
     * @param exception Exception
     * @throws IOException exception from retrieving the message body.
     * @return ErrorResponse object
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public ErrorResponse handleMessageNotReadableErrors(HttpMessageNotReadableException exception) throws IOException {
        String requestBody = IOUtils.toString(exception.getHttpInputMessage().getBody(), Charset.defaultCharset());
        log.info("Unable to read message: {}", requestBody, exception);

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .status(BAD_REQUEST.value())
                .message("Unable to read request body: " + requestBody)
                .build();
    }

}
