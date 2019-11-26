package uk.gov.dhsc.htbhf.claimant.controller.v3;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dhsc.htbhf.claimant.controller.v2.ClaimController;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimantDTOToClaimantConverter;
import uk.gov.dhsc.htbhf.claimant.converter.VoucherEntitlementToDTOConverter;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/v3/claims", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@Slf4j
@Api(description = "Endpoints for dealing with claims, e.g. persisting a claim.")
public class ClaimControllerV3 extends ClaimController {

    public ClaimControllerV3(@Qualifier("v3ClaimService") ClaimService v3ClaimService,
                             ClaimantDTOToClaimantConverter claimantConverter,
                             VoucherEntitlementToDTOConverter voucherConverter) {
        super(v3ClaimService, claimantConverter, voucherConverter);
    }

    @PostMapping
    @ApiOperation("Create or update a claim.")
    @ApiResponses({@ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class)})
    @Override
    public ResponseEntity<ClaimResultDTO> createOrUpdateClaim(@RequestBody @Valid @ApiParam("The claim to persist") ClaimDTO claimDTO) {
        log.debug("Received V3 claim");
        return super.createOrUpdateClaim(claimDTO);
    }

}
