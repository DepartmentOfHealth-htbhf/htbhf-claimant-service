package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.dhsc.htbhf.claimant.model.v2.ClaimantDTO;
import uk.gov.dhsc.htbhf.swagger.SwaggerGenerationUtil;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ClaimantServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class ClaimantServiceApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void contextLoads() {
    }

    @Test
    public void swaggerDocumentationRetrieved() throws IOException {
        SwaggerGenerationUtil.assertSwaggerDocumentationRetrieved(testRestTemplate, port);
    }

    @Test
    public void datesAreSerialisedCorrectly() throws JsonProcessingException {
        ClaimantDTO dto = ClaimantDTO.builder().dateOfBirth(LocalDate.of(1970, 1, 31)).build();

        String output = objectMapper.writeValueAsString(dto);

        assertThat(output).contains("1970-01-31");
    }
}
