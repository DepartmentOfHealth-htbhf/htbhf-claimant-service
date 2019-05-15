package uk.gov.dhsc.htbhf.claimant.model.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DepositFundsRequest {

    @JsonProperty("amountInPence")
    private Integer amountInPence;

    @JsonProperty("reference")
    private String reference;
}
