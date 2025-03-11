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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.schema.Error;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Link;
import net.es.iri.api.facility.schema.Relationships;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.ResourceType;
import net.es.iri.api.facility.utils.UrlTransform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Fun with test cases for the IRI Facility API.
 *
 * @author hacksaw
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        log.debug("[ApplicationTest::setUp] starting tests using port {}.", port);
    }

    @AfterEach
    void tearDown() {
        log.debug("[ApplicationTest::tearDown] ending tests.");
    }

    /**
     * Test the getFacility API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetFacility() throws Exception {
        log.debug("[ApplicationTest::testGetFacility] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/facility";

        // Perform the GET /facility operation.
        ResponseEntity<Facility> response = restTemplate.getForEntity(url, Facility.class);

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        Facility facility = response.getBody();
        assertEquals("09a22593-2be8-46f6-ae54-2904b04e13a4", facility.getId());

        // Did we decode the lastModified time correctly?
        assertEquals(OffsetDateTime.parse("2025-03-03T20:57:49.690-00:00"), facility.getLastModified());

        // Count the total number of links.
        assertNotNull(facility.getLinks());
        assertEquals(30, facility.getLinks().size());

        UrlTransform urlTransform = new UrlTransform("(/|http://localhost:" + port + "/)");

        // Count individual link types.
        int hasSelf = 0;
        int hasResource = 0;
        int hasEvent = 0;
        int hasIncident = 0;
        int hostedAt = 0;
        int others = 0;

        // Count and resolve each href.
        for (Link link : facility.getLinks()) {
            switch (link.getRel()) {
                case Relationships.SELF -> {
                    hasSelf++;
                    String self_url = urlTransform.getPath(link.getHref()).toUriString();
                    log.debug("[ApplicationTest::testGetFacility] self {}", self_url);
                    ResponseEntity<Facility> self =
                        restTemplate.getForEntity(self_url, Facility.class);
                    assertEquals(HttpStatus.OK, self.getStatusCode());
                    assertNotNull(self.getBody());
                    assertEquals("09a22593-2be8-46f6-ae54-2904b04e13a4", self.getBody().getId());
                }

                case Relationships.HAS_RESOURCE -> {
                    hasResource++;
                    String resource_url = urlTransform.getPath(link.getHref()).toUriString();
                    log.debug("[ApplicationTest::testGetFacility] hasResource {}", resource_url);
                    ResponseEntity<Resource> resource =
                        restTemplate.getForEntity(resource_url, Resource.class);
                    assertEquals(HttpStatus.OK, resource.getStatusCode());
                    assertNotNull(resource.getBody());
                    String uuid = resource_url.substring(resource_url.lastIndexOf("/") + 1);
                    assertEquals(uuid, resource.getBody().getId());
                }

                case Relationships.HAS_EVENT -> {
                    hasEvent++;
                    String event_url = urlTransform.getPath(link.getHref()).toUriString();
                    log.debug("[ApplicationTest::testGetFacility] hasEvent {}", event_url);
                    ResponseEntity<Event> event =
                        restTemplate.getForEntity(event_url, Event.class);
                    assertEquals(HttpStatus.OK, event.getStatusCode());
                    assertNotNull(event.getBody());
                    String uuid = event_url.substring(event_url.lastIndexOf("/") + 1);
                    assertEquals(uuid, event.getBody().getId());
                }

                case Relationships.HAS_INCIDENT -> {
                    hasIncident++;
                    String incident_url = urlTransform.getPath(link.getHref()).toUriString();
                    log.debug("[ApplicationTest::testGetFacility] hasIncident {}", incident_url);
                    ResponseEntity<Event> incident =
                        restTemplate.getForEntity(incident_url, Event.class);
                    assertEquals(HttpStatus.OK, incident.getStatusCode());
                    assertNotNull(incident.getBody());
                    String uuid = incident_url.substring(incident_url.lastIndexOf("/") + 1);
                    assertEquals(uuid, incident.getBody().getId());
                }

                case Relationships.HOSTED_AT -> {
                    hostedAt++;
                    String hosted_url = urlTransform.getPath(link.getHref()).toUriString();
                    log.debug("[ApplicationTest::testGetFacility] hostedAt {}", hosted_url);
                    ResponseEntity<Event> hosted =
                        restTemplate.getForEntity(hosted_url, Event.class);
                    assertEquals(HttpStatus.OK, hosted.getStatusCode());
                    assertNotNull(hosted.getBody());
                    String uuid = hosted_url.substring(hosted_url.lastIndexOf("/") + 1);
                    assertEquals(uuid, hosted.getBody().getId());

                }
                default -> others++;
            }
        }

        assertEquals(1, hasSelf);
        assertEquals(20, hasResource);
        assertEquals(5, hasEvent);
        assertEquals(2, hasIncident);
        assertEquals(1, hostedAt);
        assertEquals(1, others);

        log.debug("[ApplicationTest::testGetFacility] end test.");
    }

    /**
     * Test a failure of the getFacility API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetFacilityFailure() throws Exception {
        log.debug("[ApplicationTest::testGetFacilityFailure] start test.");

        // Build the bad target URL.
        String url = "http://localhost:" + port + "/api/v1/status/facility/09a22593-2be8-46f6-ae54-2904b04e13a4";

        // Perform the GET /facility operation.
        ResponseEntity<Error> response = restTemplate.getForEntity(url, Error.class);

        // Verify it failed.
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        Error error = response.getBody();
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), error.getError());

        log.debug("[ApplicationTest::testGetFacilityFailure] end test.");
    }

    /**
     * Test the getResources API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetResources() throws Exception {
        log.debug("[ApplicationTest::testGetResources] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/resources";

        // Perform the GET /resources operation.
        ResponseEntity<List<Resource>> response = restTemplate.exchange(url, HttpMethod.GET,
            null, new ParameterizedTypeReference<>() {});

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        List<Resource> resources = response.getBody();
        assertNotNull(resources);

        // Get each resource individually.
        for (Resource resource : resources) {
            String resource_url = "http://localhost:" + port + "/api/v1/status/resources/" + resource.getId();
            ResponseEntity<Resource> individual = restTemplate.getForEntity(resource_url, Resource.class);
            assertEquals(HttpStatus.OK, individual.getStatusCode());
            assertNotNull(individual.getBody());
            assertEquals(resource, individual.getBody());
        }

        // Now check if-modified-since capabilities.
        String ifms = OffsetDateTime.parse("2025-03-02T07:50:20.672Z")
            .format(DateTimeFormatter.RFC_1123_DATE_TIME);

        response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(ifms),
            new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, response.getStatusCode());

        HttpHeaders responseHeaders = response.getHeaders();
        String lastModified = responseHeaders.getFirst(HttpHeaders.LAST_MODIFIED);

        // We should get them all.
        assertNotNull(response.getBody());
        assertEquals(20, response.getBody().size());

        // Now we should get zero.
        response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(lastModified),
            new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        assertFalse(response.hasBody());

        // Query should return one less based on specific lastModified date.
        ifms = OffsetDateTime.parse("2025-03-03T07:50:40.672-00:00")
            .format(DateTimeFormatter.RFC_1123_DATE_TIME);
        response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(ifms),
            new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(19, response.getBody().size());

        log.debug("[ApplicationTest::testGetResources] end test.");
    }

    /**
     * Test the getResource API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetResource() throws Exception {
        log.debug("[ApplicationTest::testGetResource] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/resources/29989783-bc70-4cc8-880f-f2176d6cec20";
        ResponseEntity<Resource> response = restTemplate.getForEntity(url, Resource.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("iris.nersc.gov", response.getBody().getName());
        assertEquals(ResourceType.WEBSITE, response.getBody().getType());

        log.debug("[ApplicationTest::testGetResource] end test.");
    }

    /**
     * Test getResource API failure
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetResourceFailure() throws Exception {
        log.debug("[ApplicationTest::testGetResourceFailure] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/resources/bad-id";
        ResponseEntity<Error> response = restTemplate.getForEntity(url, Error.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), response.getBody().getError());

        log.debug("[ApplicationTest::testGetResourceFailure] end test.");
    }

    /**
     * Test the getIncidents API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetIncidents() throws Exception {

    }

    /**
     * Get the default headers with the specified If-Modified-Since.
     *
     * @param ifModifiedSince
     * @return
     */
    private HttpEntity<String> getHeaders(String ifModifiedSince) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
        return new HttpEntity<>(headers);
    }
}