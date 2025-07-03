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
 */
package net.es.iri.api.facility.actuator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Fun with test cases for the Spring Actuator API.
 *
 * @author hacksaw
 */
@Slf4j
@Profile("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        log.debug("[ActuatorTest::setUp] starting tests using port {}.", port);
    }

    @AfterEach
    void tearDown() {
        log.debug("[ActuatorTest::tearDown] ending tests.");
    }

    /**
     * Test the getFacility API.
     */
    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    void livenessShouldBeUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/liveness", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    void readinessShouldBeUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/readiness", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    void infoEndpointReturnsExpectedAppInfo() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/info", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        assertNotNull(body);

        assertTrue(body.contains("\"name\":\"iri-facility-status-api\""));
        assertTrue(body.contains("\"description\":\"This is Java reference implementation of the Facility Status API.\""));
        assertTrue(body.contains("\"version\":\"1.0\""));
        assertTrue(body.contains("\"type\":\"Spring Boot\""));
        assertTrue(body.contains("\"java\""));
    }
}