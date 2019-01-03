package uk.gov.dhsc.htbhf.claimant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimDTOToClaimConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimService;

import javax.validation.Valid;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController()
@RequestMapping(value = "/claim", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimDTOToClaimConverter converter;

    @PostMapping
    @ResponseStatus(CREATED)
    public void newClaim(@RequestBody @Valid ClaimDTO claimDTO) {
        log.info("Received claim");
        Claim claim = converter.convert(claimDTO);
        claimService.createClaim(claim);
    }
}
