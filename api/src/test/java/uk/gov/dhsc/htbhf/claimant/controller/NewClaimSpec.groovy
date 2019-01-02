package uk.gov.dhsc.htbhf.claimant.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.CREATED
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimDTOWithEmptySecondName
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimDTOWithFirstNameTooLong
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimDTOWithNoSecondName
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimDTOWithSecondNameTooLong
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aValidClaimDTO

@SpringBootTest(webEnvironment = RANDOM_PORT)
class NewClaimSpec extends Specification{

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    URI endpointUrl = URI.create("/claim")

    def "A new valid claim is accepted"() {
        given: "A valid claim request"
        def claim = aValidClaimDTO()

        when: "The request is received"
        ResponseEntity<Void> response = restTemplate.postForEntity(endpointUrl, claim, Void.class)

        then: "A created response is returned"
        assertThat(response.statusCode).isEqualTo(CREATED)
    }

    def "An invalid claim"(ClaimDTO claim, String expectedErrorMessage, String expectedField) {
        expect:
        ResponseEntity<ErrorAttributes> response = restTemplate.postForEntity(endpointUrl, claim, ErrorAttributes.class)
        assertThat(response.statusCode).is(BAD_REQUEST)
        assertThat(response.body).isNotNull()

        where:
        claim | expectedErrorMessage | expectedField
        aClaimDTOWithSecondNameTooLong() | "Name too long" | "secondName"
        aClaimDTOWithNoSecondName() | "Name too long" | "secondName"
        aClaimDTOWithEmptySecondName() | "Name too long" | "secondName"
        aClaimDTOWithFirstNameTooLong() | "Name too long" | "firstName"

    }
}
