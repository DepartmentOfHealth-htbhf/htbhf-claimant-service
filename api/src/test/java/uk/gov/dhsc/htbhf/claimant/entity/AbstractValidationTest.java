package uk.gov.dhsc.htbhf.claimant.entity;

import org.junit.jupiter.api.BeforeEach;

import java.nio.CharBuffer;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class AbstractValidationTest {

    // Create a string 501 characters long
    protected static final String LONG_STRING = CharBuffer.allocate(501).toString().replace('\0', 'A');

    protected Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }
}
