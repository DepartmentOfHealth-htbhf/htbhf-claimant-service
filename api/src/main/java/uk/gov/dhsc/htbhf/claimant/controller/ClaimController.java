package uk.gov.dhsc.htbhf.claimant.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimToClaimDTOConverter;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimToClaimResponseDTOConverter;
import uk.gov.dhsc.htbhf.claimant.converter.NewClaimDTOToClaimRequestConverter;
import uk.gov.dhsc.htbhf.claimant.converter.VoucherEntitlementToDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.*;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/v3/claims", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
@Api(description = "Endpoints for dealing with claims, e.g. persisting a claim.")
public class ClaimController {

    private final ClaimService claimService;
    private final VoucherEntitlementToDTOConverter voucherConverter;
    private final ClaimRepository claimRepository;
    private final ClaimToClaimDTOConverter claimToClaimDTOConverter;
    private final ClaimToClaimResponseDTOConverter claimToClaimResponseDTOConverter;
    private final NewClaimDTOToClaimRequestConverter claimRequestConverter;

    private final Map<ClaimStatus, HttpStatus> statusMap = Map.of(
            ClaimStatus.NEW, HttpStatus.CREATED,
            ClaimStatus.PENDING, HttpStatus.OK,
            ClaimStatus.ACTIVE, HttpStatus.OK,
            ClaimStatus.PENDING_EXPIRY, HttpStatus.OK,
            ClaimStatus.EXPIRED, HttpStatus.OK,
            ClaimStatus.REJECTED, HttpStatus.OK,
            ClaimStatus.ERROR, HttpStatus.INTERNAL_SERVER_ERROR
    );

    @PostMapping
    @ApiOperation("Create a claim.")
    @ApiResponses({@ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class)})
    public ResponseEntity<ClaimResultDTO> createClaim(@RequestBody @Valid @ApiParam("The claim to persist") NewClaimDTO newClaimDTO,
                                                      @RequestHeader(name = "user",defaultValue = "SYSTEM") String user) {
        log.debug("Received claim");

        ClaimRequest claimRequest = claimRequestConverter.convert(newClaimDTO);
        ClaimResult result = claimService.createClaim(claimRequest, user);

        return createResponse(result);
    }

    @GetMapping("/{id}")
    @ApiOperation("Retrieve a claim by id.")
    @ApiResponses({@ApiResponse(code = 404, message = "Claim not found", response = ErrorResponse.class)})
    public ClaimDTO retrieveClaimById(@PathVariable("id") UUID id) {
        log.debug("Retrieve claim by id {}", id);

        Claim claim = claimRepository.findClaim(id);

        return claimToClaimDTOConverter.convert(claim);
    }

    @PostMapping("/search")
    @ApiOperation("Retrieve Claims.")
    @ApiResponses({@ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class)})
    public ResponseEntity<List<ClaimResponseDTO>> retrieveAllClaims(@RequestBody @Valid @ApiParam("retrieve claims") Map<String, String> claimFilter) {
        log.debug("Retrieve claims");

        List<ClaimResponseDTO> result = claimService.findClaims();

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private ResponseEntity<ClaimResultDTO> createResponse(ClaimResult result) {
        ClaimStatus claimStatus = result.getClaim().getClaimStatus();
        HttpStatus statusCode = getHttpStatus(result);
        VoucherEntitlementDTO entitlement = getEntitlement(result);
        ClaimResultDTO body = ClaimResultDTO.builder()
                .claimStatus(claimStatus)
                .eligibilityStatus(result.getClaim().getEligibilityStatus())
                .voucherEntitlement(entitlement)
                .verificationResult(result.getVerificationResult())
                .build();
        return new ResponseEntity<>(body, statusCode);
    }

    private VoucherEntitlementDTO getEntitlement(ClaimResult result) {
        return result.getVoucherEntitlement().map(voucherConverter::convert).orElse(null);
    }

    private HttpStatus getHttpStatus(ClaimResult claimResult) {
        ClaimStatus claimStatus = claimResult.getClaim().getClaimStatus();
        HttpStatus statusCode = statusMap.get(claimStatus);
        if (statusCode == null) {
            log.warn("claim status without HttpStatus: {}", claimStatus);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return statusCode;
    }

}
