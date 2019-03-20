package uk.gov.dhsc.htbhf.smoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
class ServiceIsRunningTest {

    private static final String BASE_URL = System.getProperty("base_url") == null ? "http://localhost:8080" : System.getProperty("base_url");
    private static final String PROTOCOL = BASE_URL.startsWith("http") ? "" : "https://";
    private static final URI HEALTH_URI = URI.create(PROTOCOL + BASE_URL + "/actuator/health");

    private TestRestTemplate client = new TestRestTemplate();

    @Test
    void healthEndpointShouldReturnUp() {
        System.out.println("Endpoint is: " + HEALTH_URI.toString());
        ResponseEntity<String> response = client.getForEntity(HEALTH_URI, String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().trim()).isEqualTo("{\"status\":\"UP\"}");
    }

}
