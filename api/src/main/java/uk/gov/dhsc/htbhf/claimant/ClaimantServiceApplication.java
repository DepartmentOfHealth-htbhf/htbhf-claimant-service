package uk.gov.dhsc.htbhf.claimant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.dhsc.htbhf.requestcontext.RequestContextConfiguration;

/**
 * The starting point for spring boot, this class enables SpringFox for documenting the api using swagger
 * and defines a number of beans.
 * See also: {@link ApiDocumentation}.
 */
@SpringBootApplication
@EnableSwagger2
@Import(RequestContextConfiguration.class)
@SuppressWarnings("PMD.UseUtilityClass")
public class ClaimantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimantServiceApplication.class, args);
    }
}
