package uk.gov.dhsc.htbhf.claimant.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimService;

import javax.validation.Valid;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController()
@RequestMapping(value = "/claim", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final ConversionService conversionService;

    @PostMapping
    @ResponseStatus(CREATED)
    public void newClaim(@RequestBody @Valid ClaimDTO claimDTO) {
        Claim claim = conversionService.convert(claimDTO, Claim.class);
        claimService.createClaim(claim);
    }
}
