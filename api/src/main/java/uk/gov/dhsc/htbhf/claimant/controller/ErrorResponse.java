package uk.gov.dhsc.htbhf.claimant.controller;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Error response for api requests.
 */
@Data
@Builder
public class ErrorResponse {
    private List<FieldError> fieldErrors;
    private String requestId;
    private String timestamp;
    private Integer status;
    private String message;


    /**
     * Field errors used for to represent field validation errors.
     */
    @Data
    @Builder
    public static class FieldError {
        private String message;
        private String field;
    }
}
