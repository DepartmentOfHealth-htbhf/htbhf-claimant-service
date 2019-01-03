package uk.gov.dhsc.htbhf.claimant.controller;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Error response for api requests.
 */
@Data
@Builder
class ErrorResponse {
    List<FieldError> fieldErrors;
    String requestId;
    String timestamp;
    Integer status;
    String message;


    /**
     * Field errors used for to represent field validation errors.
     */
    @Data
    @Builder
    static class FieldError {
        private String message;
        private String field;
    }
}
