package uk.gov.dhsc.htbhf.claimant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.dhsc.htbhf.swagger.SwaggerGenerationUtil;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ClaimantServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ClaimantServiceApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void contextLoads() {
    }

    @Test
    public void swaggerDocumentationRetrieved() throws IOException {
        SwaggerGenerationUtil.assertSwaggerDocumentationRetrieved(testRestTemplate, port);
    }

}
