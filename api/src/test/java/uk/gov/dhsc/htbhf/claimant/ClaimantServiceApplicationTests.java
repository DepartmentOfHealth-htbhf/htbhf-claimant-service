package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.io.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

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
        String response = testRestTemplate.getForObject(buildUrl(), String.class);

        String jsonAsYaml = convertJsonToYaml(response);

        assertThat(jsonAsYaml).isNotBlank();
        assertThat(jsonAsYaml).containsSequence("swagger: \"2.0\"");

        File swaggerFile = new File("../swagger.yml");
        Files.asCharSink(swaggerFile, Charset.defaultCharset()).write(jsonAsYaml);
    }

    private String convertJsonToYaml(String response) throws IOException {
        JsonNode jsonNodeTree = new ObjectMapper().readTree(response);
        return new YAMLMapper()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .writeValueAsString(jsonNodeTree);
    }

    private String buildUrl() {
        return "http://localhost:" + port + "/v2/api-docs";
    }

}
