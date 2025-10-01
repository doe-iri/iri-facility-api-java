/*
 * IRI Facility Status API reference implementation Copyright (c) 2025,
 * The Regents of the University of California, through Lawrence
 * Berkeley National Laboratory (subject to receipt of any required
 * approvals from the U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.iri.api.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.schema.Error;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.utils.UrlTransform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Fun with test cases for the IRI Facility API.
 *
 * @author hacksaw
 */
@Slf4j
@Profile("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FacilityEndpointTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        log.debug("[FacilityEndpointTest::setUp] starting tests using port {}.", port);
    }

    @AfterEach
    void tearDown() {
        log.debug("[FacilityEndpointTest::tearDown] ending tests.");
    }

    /**
     * Test the getFacility API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetFacility() throws Exception {
        log.debug("[FacilityEndpointTest::testGetFacility] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/facility";

        RestClient client = RestClient.create();

        // Perform the GET /facility operation.
        ResponseEntity<Facility> response = client.get().uri(url).retrieve().toEntity(Facility.class);

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        Facility facility = response.getBody();
        assertEquals("09a22593-2be8-46f6-ae54-2904b04e13a4", facility.getId());

        // Did we decode the lastModified time correctly?
        assertEquals(OffsetDateTime.parse("2025-07-24T02:31:08Z"), facility.getLastModified());

        UrlTransform urlTransform = new UrlTransform("(http://localhost:8081/|http://localhost:" + port + "/)");

        // Verify the selfUri member points to the target facility.
        String self_url = urlTransform.getPath(facility.getSelfUri()).toUriString();
        log.debug("[FacilityEndpointTest::testGetFacility] self_url = {}", self_url);

        ResponseEntity<Facility> self = client.get().uri(self_url).retrieve().toEntity(Facility.class);
        assertEquals(HttpStatus.OK, self.getStatusCode());
        assertNotNull(self.getBody());
        assertEquals("09a22593-2be8-46f6-ae54-2904b04e13a4", self.getBody().getId());

        // Count and resolve resource href.
        assertEquals(20, facility.getResourceUris().size());
        assertEquals(4005, facility.getEventUris().size());
        assertEquals(403, facility.getIncidentUris().size());
        assertEquals(1, facility.getSiteUris().size());
        assertEquals(1, facility.getLocationUris().size());

        log.debug("[FacilityEndpointTest::testGetFacility] end test.");
    }

    /**
     * Test a failure of the getFacility API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetFacilityFailure() throws Exception {
        log.debug("[FacilityEndpointTest::testGetFacilityFailure] start test.");

        RestClient client = RestClient.create();

        // Build the bad target URL.
        String url = "http://localhost:" + port + "/api/v1/facility/09a22593-2be8-46f6-ae54-2904b04e13a4";

        // Perform the GET /facility operation.
        ResponseEntity<Error> error = client.get().uri(url).retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                // Verify it failed as expected.
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new IllegalStateException("Unexpected server error: " + response.getStatusCode());
            })
            // We don't expect a body for 404, but this triggers the exchange
            .toEntity(Error.class);

        // Verify it failed.
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertNotNull(error.getBody());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), error.getBody().getTitle());

        log.debug("[FacilityEndpointTest::testGetFacilityFailure] end test.");
    }

    /**
     * Get the default headers with the specified If-Modified-Since.
     *
     * @param ifModifiedSince
     * @return
     */
    private HttpHeaders getHeaders(String ifModifiedSince) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
        return headers;
    }
}