package uk.gov.dhsc.htbhf.claimant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import static java.util.Collections.emptyList;

/**
 * Definitions of SpringFox beans to generate Swagger documentation.
 * Also logs application info at startup.
 */
@Configuration
@Import(BeanValidatorPluginsConfiguration.class) // enable documentation of JSR-305 constraints
@Slf4j
public class ApiDocumentation {

    @Value("${app.version:}") // use APP_VERSION env variable if available, otherwise give no version info
    private String appVersion;

    @Bean
    public Docket apiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .host("N/A")
                .select()
                .apis(RequestHandlerSelectors.basePackage(this.getClass().getPackageName()))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(getApiInfo());
    }

    private ApiInfo getApiInfo() {
        return new ApiInfo(
                "Claimant Service",
                "Responsible for the persistence and retrieval of Claims (and Claimants) for Help To Buy Healthy Food",
                appVersion,
                "",
                new Contact("Department Of Health", "https://github.com/DepartmentOfHealth-htbhf", "dh-htbhf-team@equalexperts.com"),
                "MIT",
                "https://opensource.org/licenses/MIT",
                emptyList()
        );

    }
}
