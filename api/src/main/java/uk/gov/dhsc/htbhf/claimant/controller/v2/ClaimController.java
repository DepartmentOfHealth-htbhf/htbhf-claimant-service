package uk.gov.dhsc.htbhf.claimant.controller.v2;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimantDTOToClaimantConverter;
import uk.gov.dhsc.htbhf.claimant.converter.VoucherEntitlementToDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;
import uk.gov.dhsc.htbhf.claimant.model.v2.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import java.util.Map;
import javax.validation.Valid;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController()
@RequestMapping(value = "/v2/claims", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
@Api(description = "Endpoints for dealing with claims, e.g. persisting a claim.")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimantDTOToClaimantConverter claimantConverter;
    private final VoucherEntitlementToDTOConverter voucherConverter;

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
    @ApiOperation("Create or update a claim.")
    @ApiResponses({@ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class)})
    public ResponseEntity<ClaimResultDTO> createOrUpdateClaim(@RequestBody @Valid @ApiParam("The claim to persist") ClaimDTO claimDTO) {
        log.debug("Received claim");
        Claimant claimant = claimantConverter.convert(claimDTO.getClaimant());
        ClaimRequest claimRequest = ClaimRequest.builder()
                .claimant(claimant)
                .deviceFingerprint(claimDTO.getDeviceFingerprint())
                .webUIVersion(claimDTO.getWebUIVersion())
                .build();
        ClaimResult result = claimService.createOrUpdateClaim(claimRequest);

        return createResponse(result);
    }

    private ResponseEntity<ClaimResultDTO> createResponse(ClaimResult result) {
        ClaimStatus claimStatus = result.getClaim().getClaimStatus();
        HttpStatus statusCode = getHttpStatus(result);
        VoucherEntitlementDTO entitlement = getEntitlement(result);
        ClaimResultDTO body = ClaimResultDTO.builder()
                .claimStatus(claimStatus)
                .eligibilityStatus(result.getClaim().getEligibilityStatus())
                .voucherEntitlement(entitlement)
                .claimUpdated(result.getClaimUpdated())
                .updatedFields(result.getUpdatedFields())
                .verificationResult(result.getVerificationResult())
                .build();
        return new ResponseEntity<>(body, statusCode);
    }

    private VoucherEntitlementDTO getEntitlement(ClaimResult result) {
        return result.getVoucherEntitlement().map(voucherConverter::convert).orElse(null);
    }

    private HttpStatus getHttpStatus(ClaimResult claimResult) {
        if (isTrue(claimResult.getClaimUpdated())) {
            return HttpStatus.OK;
        }
        ClaimStatus claimStatus = claimResult.getClaim().getClaimStatus();
        HttpStatus statusCode = statusMap.get(claimStatus);
        if (statusCode == null) {
            log.warn("claim status without HttpStatus: {}", claimStatus);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return statusCode;
    }
}
