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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.schema.Error;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.Location;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.Site;
import net.es.iri.api.facility.utils.UrlTransform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
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
class StatusEndpointTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        log.debug("[StatusEndpointTest::setUp] starting tests using port {}.", port);
    }

    @AfterEach
    void tearDown() {
        log.debug("[StatusEndpointTest::tearDown] ending tests.");
    }

    /**
     * Test the getFacility API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetFacility() throws Exception {
        log.debug("[StatusEndpointTest::testGetFacility] start test.");

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
        ResponseEntity<Facility> self = client.get().uri(self_url).retrieve().toEntity(Facility.class);
        assertEquals(HttpStatus.OK, self.getStatusCode());
        assertNotNull(self.getBody());
        assertEquals("09a22593-2be8-46f6-ae54-2904b04e13a4", self.getBody().getId());

        // Count and resolve resource href.
        int hasResource = 0;
        for (String uri : facility.getResourceUris()) {
            hasResource++;
            String resource_url = urlTransform.getPath(uri).toUriString();
            ResponseEntity<Resource> resource = client.get().uri(resource_url).retrieve().toEntity(Resource.class);
            assertEquals(HttpStatus.OK, resource.getStatusCode());
            assertNotNull(resource.getBody());
            String uuid = resource_url.substring(resource_url.lastIndexOf("/") + 1);
            assertEquals(uuid, resource.getBody().getId());
        }

        assertEquals(20, hasResource);

        int hasEvent = 0;
        for (String uri : facility.getEventUris()) {
            hasEvent++;
            String event_url = urlTransform.getPath(uri).toUriString();
            ResponseEntity<Event> event = client.get().uri(event_url).retrieve().toEntity(Event.class);
            assertEquals(HttpStatus.OK, event.getStatusCode());
            assertNotNull(event.getBody());
            String uuid = event_url.substring(event_url.lastIndexOf("/") + 1);
            assertEquals(uuid, event.getBody().getId());
        }

        assertEquals(4005, hasEvent);

        int hasIncident = 0;
        for (String uri : facility.getIncidentUris()) {
            hasIncident++;
            String incident_url = urlTransform.getPath(uri).toUriString();
            ResponseEntity<Incident> incident = client.get().uri(incident_url).retrieve().toEntity(Incident.class);
            assertEquals(HttpStatus.OK, incident.getStatusCode());
            assertNotNull(incident.getBody());
            String uuid = incident_url.substring(incident_url.lastIndexOf("/") + 1);
            assertEquals(uuid, incident.getBody().getId());
        }

        assertEquals(403, hasIncident);

        int hostedAt = 0;
        for (String uri : facility.getSiteUris()) {
            hostedAt++;
            String hosted_url = urlTransform.getPath(uri).toUriString();
            ResponseEntity<Site> hosted = client.get().uri(hosted_url).retrieve().toEntity(Site.class);
            assertEquals(HttpStatus.OK, hosted.getStatusCode());
            assertNotNull(hosted.getBody());
            String uuid = hosted_url.substring(hosted_url.lastIndexOf("/") + 1);
            assertEquals(uuid, hosted.getBody().getId());
        }

        assertEquals(1, hostedAt);

        int hasLocation = 0;
        for (String uri : facility.getLocationUris()) {
            hasLocation++;
            String location_url = urlTransform.getPath(uri).toUriString();
            log.debug("Loading location: {}", location_url);
            ResponseEntity<Location> location = client.get().uri(location_url).retrieve().toEntity(Location.class);
            assertEquals(HttpStatus.OK, location.getStatusCode());
            assertNotNull(location.getBody());
            String uuid = location_url.substring(location_url.lastIndexOf("/") + 1);
            assertEquals(uuid, location.getBody().getId());
        }

        assertEquals(1, hasLocation);

        log.debug("[StatusEndpointTest::testGetFacility] end test.");
    }

    /**
     * Test a failure of the getFacility API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetFacilityFailure() throws Exception {
        log.debug("[StatusEndpointTest::testGetFacilityFailure] start test.");

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

        log.debug("[StatusEndpointTest::testGetFacilityFailure] end test.");
    }

    /**
     * Test the getResources API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetResources() throws Exception {
        log.debug("[StatusEndpointTest::testGetResources] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/resources";

        // Perform the GET /resources operation.
        RestClient client = RestClient.create();
        ResponseEntity<List<Resource>> response = client.get().uri(url).retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});

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
            ResponseEntity<Resource> individual = client.get().uri(resource_url).retrieve().toEntity(Resource.class);
            assertEquals(HttpStatus.OK, individual.getStatusCode());
            assertNotNull(individual.getBody());
            assertEquals(resource, individual.getBody());
        }

        // Now check if-modified-since capabilities.
        response = client.get().uri(url)
            .headers(h -> h.addAll(getHeaders(OffsetDateTime.parse("2025-03-02T07:50:20.672Z").format(DateTimeFormatter.RFC_1123_DATE_TIME))))
            .retrieve().toEntity(new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, response.getStatusCode());

        HttpHeaders responseHeaders = response.getHeaders();
        String lastModified = responseHeaders.getFirst(HttpHeaders.LAST_MODIFIED);

        // We should get them all.
        assertNotNull(response.getBody());
        assertEquals(20, response.getBody().size());

        // Now we should get zero.
        response = client.get().uri(url)
            .headers(h -> h.addAll(getHeaders(lastModified)))
            .retrieve().toEntity(new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        assertFalse(response.hasBody());

        // Query should return one less based on the specific lastModified date.
        response = client.get().uri(url)
            .headers(h -> h.addAll(getHeaders(OffsetDateTime.parse("2025-03-03T07:50:40.672-00:00").format(DateTimeFormatter.RFC_1123_DATE_TIME))))
            .retrieve().toEntity(new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20, response.getBody().size());

        log.debug("[StatusEndpointTest::testGetResources] end test.");
    }

    /**
     * Test the getResource API.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetResource() throws Exception {
        log.debug("[StatusEndpointTest::testGetResource] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/resources/29989783-bc70-4cc8-880f-f2176d6cec20";

        // Perform the GET /facility operation.
        RestClient client = RestClient.create();
        ResponseEntity<Resource> response = client.get().uri(url).retrieve().toEntity(Resource.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("iris.nersc.gov", response.getBody().getName());
        //assertEquals(ResourceType.WEBSITE, response.getBody().getType());

        log.debug("[StatusEndpointTest::testGetResource] end test.");
    }

    /**
     * Test getResource API failure
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetResourceFailure() throws Exception {
        log.debug("[StatusEndpointTest::testGetResourceFailure] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/resources/bad-id";
        RestClient client = RestClient.create();
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

        log.debug("[StatusEndpointTest::testGetResourceFailure] end test.");
    }

    /**
     * Test the getIncidents API for the "from" query parameter.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetIncidentsFrom() throws Exception {
        log.debug("[StatusEndpointTest::testGetIncidentsFrom] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/incidents";

        // Perform the GET /incidents operation.
        RestClient client = RestClient.create();
        ResponseEntity<List<Incident>> response = client.get().uri(url).retrieve()
            .toEntity(new ParameterizedTypeReference<List<Incident>>() {});

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        List<Incident> incidents = response.getBody();
        assertNotNull(incidents);

        // Filter incidents where the start time is equal to or after the specified startTime
        List<Incident> filteredIncidents = incidents.stream()
            .filter(incident -> incident.isConflict("2025-06-27T06:04:25.275Z", null))
            .sorted(Comparator.comparing((Incident i) -> Optional.ofNullable(i.getStart()).orElse(OffsetDateTime.MIN)))
            .toList();

        // Now we want to compare this to a similar list returned from a query.
        String url_start = "http://localhost:" + port + "/api/v1/status/incidents?from=2025-06-27T06:04:25.275Z";

        // Perform the GET /incidents operation.
        response = client.get().uri(url_start).retrieve()
            .toEntity(new ParameterizedTypeReference<List<Incident>>() {});

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        List<Incident> incidentsStart = response.getBody();
        assertNotNull(incidentsStart);

        List<Incident> sortedIncidents = incidentsStart.stream()
            .sorted(Comparator.comparing(Incident::getStart))
            .toList();

        log.debug("[StatusEndpointTest::testGetIncidentsFrom] incidents = {}, sortedIncidents = {}, filteredIncidents = {}",
            incidents.size(), sortedIncidents.size(), filteredIncidents.size());

        assertEquals(filteredIncidents.size(), sortedIncidents.size());

        log.debug("[StatusEndpointTest::testGetIncidentsFrom] ending test.");
    }

    /**
     * Test the getIncidents API for "to" query parameter.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetIncidentsTo() throws Exception {
        log.debug("[StatusEndpointTest::testGetIncidentsTo] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/incidents";

        // Perform the GET /incidents operation.
        RestClient client = RestClient.create();
        ResponseEntity<List<Incident>> response = client.get().uri(url).retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        List<Incident> incidents = response.getBody();
        assertNotNull(incidents);

        // Filter incidents where the start time is equal to or after the specified startTime
        List<Incident> filteredIncidents = incidents.stream()
            .filter(incident -> incident.isConflict(null, "2025-06-28T06:04:25.275Z"))
            .sorted(Comparator.comparing((Incident i) -> Optional.ofNullable(i.getStart()).orElse(OffsetDateTime.MIN)))
            .toList();

        // Now we want to compare this to a similar list returned from a query.
        String url_start = "http://localhost:" + port + "/api/v1/status/incidents?to=2025-06-28T06:04:25.275Z";

        // Perform the GET /resources operation.
        response = client.get().uri(url_start).retrieve()
            .toEntity(new ParameterizedTypeReference<List<Incident>>() {});

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        List<Incident> incidentsStart = response.getBody();
        assertNotNull(incidentsStart);

        List<Incident> sortedIncidents = incidentsStart.stream()
            .sorted(Comparator.comparing(Incident::getStart))
            .toList();

        log.debug("[StatusEndpointTest::testGetIncidentsTo] incidents = {}, sortedIncidents = {}, filteredIncidents = {}",
            incidents.size(), sortedIncidents.size(), filteredIncidents.size());

        assertEquals(filteredIncidents.size(), sortedIncidents.size());

        log.debug("[StatusEndpointTest::testGetIncidentsTo] ending test.");
    }

    /**
     * Test the getIncidents API for "to" query parameter.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetIncidentsToFrom() throws Exception {
        log.debug("[StatusEndpointTest::testGetIncidentsToFrom] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port + "/api/v1/status/incidents";

        // Perform the GET /incidents operation.
        RestClient client = RestClient.create();
        ResponseEntity<List<Incident>> response = client.get().uri(url).retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        List<Incident> incidents = response.getBody();
        assertNotNull(incidents);

        // Filter incidents where the start time is equal to or after the specified startTime
        List<Incident> filteredIncidents = incidents.stream()
            .filter(incident -> incident.isConflict("2025-06-27T06:04:25.275Z", "2025-06-28T06:04:25.275Z"))
            .sorted(Comparator.comparing((Incident i) -> Optional.ofNullable(i.getStart()).orElse(OffsetDateTime.MIN)))
            .toList();

        // Now we want to compare this to a similar list returned from a query.
        String url_start = "http://localhost:" + port + "/api/v1/status/incidents?from=2025-06-27T06:04:25.275Z&to=2025-06-28T06:04:25.275Z";

        // Perform the GET /incidents operation.
        response = client.get().uri(url_start).retrieve().toEntity(new ParameterizedTypeReference<>() {});

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        List<Incident> incidentsStart = response.getBody();
        assertNotNull(incidentsStart);

        List<Incident> sortedIncidents = incidentsStart.stream()
            .sorted(Comparator.comparing(Incident::getStart))
            .toList();

        log.debug("[StatusEndpointTest::testGetIncidentsToFrom] incidents = {}, sortedIncidents = {}, filteredIncidents = {}",
            incidents.size(), sortedIncidents.size(), filteredIncidents.size());

        assertEquals(filteredIncidents.size(), sortedIncidents.size());

        log.debug("[StatusEndpointTest::testGetIncidentsToFrom] ending test.");
    }

    /**
     * Test the getIncidents API for "resources" query parameter.
     *
     * @throws Exception When stuff gets janky.
     */
    @Test
    public void testGetIncidentsWithResource() throws Exception {
        log.debug("[StatusEndpointTest::testGetIncidentsWithResource] start test.");

        // Build the target URL.
        String url = "http://localhost:" + port +
            "/api/v1/status/incidents?resources=29989783-bc70-4cc8-880f-f2176d6cec20,057c3750-4ba1-4b51-accf-b160be683d80";

        // Perform the GET /incidents operation.
        RestClient client = RestClient.create();
        ResponseEntity<List<Incident>> response = client.get().uri(url).retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});

        // Verify it was successful.
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // We have a result.
        assertNotNull(response.getBody());

        // It is the facility we were expecting.
        List<Incident> incidents = response.getBody();
        assertNotNull(incidents);

        log.debug("[StatusEndpointTest::testGetIncidentsWithResource] incidents = {}", incidents.size());

        assertEquals(204, incidents.size());

        log.debug("[StatusEndpointTest::testGetIncidentsWithResource] ending test.");
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