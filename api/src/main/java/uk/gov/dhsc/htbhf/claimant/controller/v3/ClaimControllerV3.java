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
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.v2.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.v2.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.v2.ClaimantDTO;
import uk.gov.dhsc.htbhf.claimant.model.v3.AddressDTOV3;
import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimDTOV3;
import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimantDTOV3;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/v3/claims", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@Slf4j
@Api(description = "Endpoints for dealing with claims, e.g. persisting a claim.")
public class ClaimControllerV3 {

    private final ClaimController claimController;

    public ClaimControllerV3(@Qualifier("v3ClaimService") ClaimService v3ClaimService,
                             ClaimantDTOToClaimantConverter claimantConverter,
                             VoucherEntitlementToDTOConverter voucherConverter) {
        //TODO MRS 28/11/2019: This is a temporary (ugly) work around to make sure we reuse the existing code without duplication, it will be replaced
        claimController = new ClaimController(v3ClaimService, claimantConverter, voucherConverter);
    }

    @PostMapping
    @ApiOperation("Create a V3 claim.")
    @ApiResponses({@ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class)})
    public ResponseEntity<ClaimResultDTO> createClaimV3(@RequestBody @Valid @ApiParam("The claim to persist") ClaimDTOV3 claimDTOV3) {
        log.debug("Received V3 claim");
        return claimController.createClaim(buildClaimDTO(claimDTOV3));
    }

    private ClaimDTO buildClaimDTO(ClaimDTOV3 v3Claim) {
        return ClaimDTO.builder()
                .deviceFingerprint(v3Claim.getDeviceFingerprint())
                .webUIVersion(v3Claim.getWebUIVersion())
                .claimant(buildClaimantDTO(v3Claim.getClaimant()))
                .build();
    }

    private ClaimantDTO buildClaimantDTO(ClaimantDTOV3 v3Claimant) {
        return ClaimantDTO.builder()
                .firstName(v3Claimant.getFirstName())
                .lastName(v3Claimant.getLastName())
                .nino(v3Claimant.getNino())
                .dateOfBirth(v3Claimant.getDateOfBirth())
                .emailAddress(v3Claimant.getEmailAddress())
                .phoneNumber(v3Claimant.getPhoneNumber())
                .address(buildAddressDTO(v3Claimant.getAddress()))
                .expectedDeliveryDate(v3Claimant.getExpectedDeliveryDate())
                .childrenDob(v3Claimant.getChildrenDob())
                .build();
    }

    private AddressDTO buildAddressDTO(AddressDTOV3 v3Address) {
        return AddressDTO.builder()
                .addressLine1(v3Address.getAddressLine1())
                .addressLine2(v3Address.getAddressLine2())
                .townOrCity(v3Address.getTownOrCity())
                .county(v3Address.getCounty())
                .postcode(v3Address.getPostcode())
                .build();
    }

}
