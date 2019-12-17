package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LetterType {

    UPDATE_YOUR_ADDRESS("518e6e56-32db-4a8c-ad7a-7baa99777f79");

    private final String templateId;
}
