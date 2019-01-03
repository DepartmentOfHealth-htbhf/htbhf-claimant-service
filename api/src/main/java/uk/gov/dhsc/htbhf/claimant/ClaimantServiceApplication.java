package uk.gov.dhsc.htbhf.claimant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.dhsc.htbhf.claimant.requestcontext.RequestContext;

@SpringBootApplication
public class ClaimantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimantServiceApplication.class, args);
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public RequestContext requestContext() {
        return new RequestContext();
    }
}
