package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum containing all the types of texts we currently have setup, each one should have its templateId
 * as stated in the Notify template.
 */
@Getter
@RequiredArgsConstructor
public enum TextType {

    INSTANT_SUCCESS_TEXT("a88f0e8b-7633-4d94-98c9-2cdbdcfdb4a6");

    private final String templateId;
}
