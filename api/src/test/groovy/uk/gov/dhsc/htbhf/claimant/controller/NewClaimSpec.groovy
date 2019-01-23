package uk.gov.dhsc.htbhf.claimant.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import spock.lang.Specification
import spock.lang.Unroll
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository

import java.time.LocalDate

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.CREATED
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO

@SpringBootTest(webEnvironment = RANDOM_PORT)
class NewClaimSpec extends Specification {

    private static final String LONG_NAME = "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" + //100
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" + //200
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" + //300
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" + //400
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" +
            "This name is way too long" + //500
            "This name is way too long"

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    ClaimantRepository claimantRepository

    URI endpointUrl = URI.create("/v1/claims")

    def "A new valid claim is accepted"() {
        given: "A valid claim request"
        def claim = aValidClaimDTO()

        when: "The request is received"
        def response = restTemplate.exchange(buildRequestEntity(claim), Void.class)

        then: "A created response is returned"
        assertThat(response.statusCode).isEqualTo(CREATED)

        and: "The claim is persisted"
        def claims = claimantRepository.findAll()
        def persistedClaim = claims.iterator().next()
        assertThat(claims).size().isEqualTo(1)
        assertThat(persistedClaim.nino).isEqualTo(claim.claimant.nino)
        assertThat(persistedClaim.firstName).isEqualTo(claim.claimant.firstName)
        assertThat(persistedClaim.lastName).isEqualTo(claim.claimant.lastName)
        assertThat(persistedClaim.dateOfBirth).isEqualTo(claim.claimant.dateOfBirth)
    }

    @Unroll
    def "Field [#fieldName] with invalid value [#value] on a claim returns the correct error response"(String fieldName, String value, String expectedErrorMessage, String expectedField) {
        expect:
        def claim = createClaimWithProperty(fieldName, value)
        def response = restTemplate.exchange(buildRequestEntity(claim), ErrorResponse.class)
        assertValidationResponse(response, expectedField, expectedErrorMessage)

        where:
        fieldName     | value         | expectedErrorMessage                       | expectedField
        "lastName"    | LONG_NAME     | "size must be between 1 and 500"           | "claimant.lastName"
        "lastName"    | null          | "must not be null"                         | "claimant.lastName"
        "lastName"    | ""            | "size must be between 1 and 500"           | "claimant.lastName"
        "firstName"   | LONG_NAME     | "size must be between 0 and 500"           | "claimant.firstName"
        "nino"        | null          | "must not be null"                         | "claimant.nino"
        "nino"        | ""            | "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"" | "claimant.nino"
        "nino"        | "YYHU456781"  | "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"" | "claimant.nino"
        "nino"        | "888888888"   | "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"" | "claimant.nino"
        "nino"        | "ABCDEFGHI"   | "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"" | "claimant.nino"
        "nino"        | "ZQQ123456CZ" | "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"" | "claimant.nino"
        "nino"        | "QQ123456T"   | "must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"" | "claimant.nino"
        "dateOfBirth" | "2099-12-31"  | "must be a past date"                      | "claimant.dateOfBirth"
    }

    @Unroll
    def "Field [#fieldName] with invalid date [#dateString] returns an error response"(String dateString, String fieldName) {
        expect:
        def claimWithInvalidDate = modifyFieldOnClaimantInJson(aValidClaimDTO(), fieldName, dateString)
        def response = restTemplate.exchange(buildRequestEntity(claimWithInvalidDate), ErrorResponse.class)
        assertErrorResponse(response, "claimant.dateOfBirth", "'${dateString}' could not be parsed as a LocalDate", "The request could not be parsed.")

        where:
        dateString   | fieldName
        "29-11-1909" | "dateOfBirth"
        "1999/12/31" | "dateOfBirth"
        "Foo"        | "dateOfBirth"
    }

    private String modifyFieldOnClaimantInJson(Object originalValue, String fieldName, String newValue) {
        String json = objectMapper.writeValueAsString(originalValue)
        JSONObject jsonObject = new JSONObject(json)
        jsonObject.getJSONObject("claimant").put(fieldName, newValue)
        return jsonObject.toString()
    }

    def "An empty claim returns an error response"() {
        given: "An empty claim request"
        def claim = "{}"

        when: "The request is received"
        def response = restTemplate.exchange(buildRequestEntity(claim), ErrorResponse.class)

        then: "A created response is returned"
        assertValidationResponse(response, "claimant", "must not be null")
    }

    private RequestEntity buildRequestEntity(Object requestObject) {
        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        return new RequestEntity<>(requestObject, headers, HttpMethod.POST, endpointUrl)
    }

    private void assertValidationResponse(ResponseEntity<ErrorResponse> response, String expectedField, String expectedFieldMessage) {
        assertErrorResponse(response, expectedField, expectedFieldMessage, "There were validation issues with the request.")
    }

    private void assertErrorResponse(ResponseEntity<ErrorResponse> response, String expectedField, String expectedFieldMessage, String expectedErrorMessage) {
        assertThat(response.statusCode).isEqualTo(BAD_REQUEST)
        assertThat(response.body.fieldErrors.size()).isEqualTo(1)
        assertThat(response.body.fieldErrors[0].field).isEqualTo(expectedField)
        assertThat(response.body.fieldErrors[0].message).isEqualTo(expectedFieldMessage)
        assertThat(response.body.requestId).isNotNull()
        assertThat(response.body.timestamp).isNotNull()
        assertThat(response.body.status).isEqualTo(BAD_REQUEST.value())
        assertThat(response.body.message).isEqualTo(expectedErrorMessage)
    }

    //This needs to be done in its own method because Spock won't allow this to be done it a test method.
    private ClaimDTO createClaimWithProperty(String fieldName, String value) {
        def claimDTO = aValidClaimDTO()
        Object valueToSet = isLocalDateField(fieldName) ? LocalDate.parse(value) : value;
        claimDTO.claimant."$fieldName" = valueToSet
        return claimDTO
    }

    private boolean isLocalDateField(String fieldName) {
        fieldName.equalsIgnoreCase("dateOfBirth")
    }
}
