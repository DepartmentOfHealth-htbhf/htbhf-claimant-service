package uk.gov.dhsc.htbhf.claimant.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import uk.gov.dhsc.htbhf.claimant.entity.Claim
import uk.gov.dhsc.htbhf.claimant.service.ClaimService

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import static org.springframework.http.HttpStatus.NOT_FOUND
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ErrorSpec extends Specification{

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @MockBean
    ClaimService claimService

    URI endpointUrl = URI.create("/v1/claims")

    def "Internal service errors return an error response"() {
        given: "A valid claim request"
        def claim = aValidClaimDTO()

        when: "An internal error occurs"
        when(claimService.createClaim(any(Claim.class) as Claim)).thenThrow(new RuntimeException())
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(endpointUrl, claim, ErrorResponse.class)

        then: "An error response is returned"
        assertThat(response.statusCode).isEqualTo(INTERNAL_SERVER_ERROR)
        assertThat(response.body.message).isEqualTo("An internal server error occurred")
        assertThat(response.body.status).isEqualTo(INTERNAL_SERVER_ERROR.value())
        assertThat(response.body.timestamp).isNotNull()
        assertThat(response.body.requestId).isNotNull()
    }

    def "Going to a non-existent path returns a 404 error"() {
        when: "Navigating to a non-existence path"
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/missing-resource", ErrorResponse.class)

        then: "A 404 error is returned"
        assertThat(response.statusCode).isEqualTo(NOT_FOUND)
    }

    def "An empty body returns an error"() {
        given: "An empty request body"
        def requestBody = ""
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON)
        def requestEntity = new RequestEntity<>(requestBody, headers, HttpMethod.POST, endpointUrl)

        when: "The request is received"
        def response = restTemplate.exchange(requestEntity, ErrorResponse.class)

        then: "An error is returned"
        assertThat(response.statusCode).isEqualTo(BAD_REQUEST)
        assertThat(response.body.timestamp).isNotNull()
        assertThat(response.body.requestId).isNotEmpty()
    }
}
