package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Claim {

    private Claimant claimant;
}
