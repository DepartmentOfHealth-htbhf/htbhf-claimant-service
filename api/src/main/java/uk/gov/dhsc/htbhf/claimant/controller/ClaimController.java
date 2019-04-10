package uk.gov.dhsc.htbhf.claimant.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponse;
import uk.gov.dhsc.htbhf.claimant.service.ClaimService;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import java.util.Map;
import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController()
@RequestMapping(value = "/v1/claims", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
@Api(description = "Endpoints for dealing with claims, e.g. persisting a claim.")
public class ClaimController {

    private final ClaimService claimService;
    private final Map<EligibilityStatus, HttpStatus> statusMap = Map.of(
            EligibilityStatus.ELIGIBLE, HttpStatus.CREATED,
            EligibilityStatus.INELIGIBLE, HttpStatus.OK,
            EligibilityStatus.PENDING, HttpStatus.OK,
            EligibilityStatus.NO_MATCH, HttpStatus.NOT_FOUND,
            EligibilityStatus.DUPLICATE, HttpStatus.OK,
            EligibilityStatus.ERROR, HttpStatus.INTERNAL_SERVER_ERROR
    );

    @PostMapping
    @ApiOperation("Persist a new claim.")
    @ApiResponses({@ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class)})
    public ResponseEntity<ClaimResponse> newClaim(@RequestBody @Valid @ApiParam("The claim to persist") ClaimDTO claimDTO) {
        log.debug("Received claim");
        Claimant claimant = claimService.createClaim(claimDTO);

        return createResponseFromClaimant(claimant);
    }

    private ResponseEntity<ClaimResponse> createResponseFromClaimant(Claimant claimant) {
        EligibilityStatus eligibilityStatus = claimant.getEligibilityStatus();
        HttpStatus statusCode = getHttpStatus(eligibilityStatus);
        ClaimResponse body = ClaimResponse.builder().eligibilityStatus(eligibilityStatus).build();
        return new ResponseEntity<>(body, statusCode);
    }

    private HttpStatus getHttpStatus(EligibilityStatus eligibilityStatus) {
        HttpStatus statusCode = statusMap.get(eligibilityStatus);
        if (statusCode == null) {
            log.warn("eligibility status without HttpStatus: {}", eligibilityStatus);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return statusCode;
    }
}
