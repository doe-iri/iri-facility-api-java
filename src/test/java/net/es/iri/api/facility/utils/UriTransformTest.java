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
package net.es.iri.api.facility.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

/**
 * Test methods in the UriTransformTest class definition.
 *
 * @author hacksaw
 */
@Slf4j
@Profile("test")
public class UriTransformTest {
    @Test
    public void proxySubstitutionTest() {
        UrlTransform transform = new UrlTransform("(https://localhost:8443|https://iri.es.net:443)");

        assertEquals("https://iri.es.net:443/account",
            transform.getPath("https://localhost:8443/account").toUriString());
    }

    @Test
    public void rootSubstitutionTest() {
        UrlTransform transform = new UrlTransform("(/|https://localhost:8443)");

        assertEquals("https://localhost:8443/account",
            transform.getPath("/account").toUriString());
    }

    @Test
    public void nullSubstitutionTest() {
        StringBuilder url = new StringBuilder("(");
        url.append("https://localhost:8443");
        url.append("|");
        url.append("https://localhost:8443");
        url.append(")");
        UrlTransform transform = new UrlTransform("(https://localhost:8443|https://localhost:8443)");

        assertEquals("https://localhost:8443/account",
            transform.getPath("https://localhost:8443/account").toUriString());
    }
}
