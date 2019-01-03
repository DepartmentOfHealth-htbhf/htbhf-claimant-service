package uk.gov.dhsc.htbhf.smoke

import groovyx.net.http.RESTClient
import spock.lang.Shared
import spock.lang.Specification

class ServiceIsRunningSpec extends Specification {

    @Shared
    String baseUrl = System.getProperty("base_url") == null ? "http://localhost:8080" : System.getProperty("base_url")
    @Shared
    String protocol = baseUrl.startsWith("http") ? "" : "https://"
    @Shared
    def client = new RESTClient(protocol + baseUrl)

    def "Health check status is UP"() {
        given: "The base url of the api"
        println("Running health check against $protocol$baseUrl")

        when: "a rest call is performed to the health check"
        def response = client.get(path : "/actuator/health")

        then: "the correct status is returned"
        with(response) {
            status == 200
            data.text.contains('"status":"UP"')
        }
    }

}