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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

/**
 * Test methods in the Common class definition.
 *
 * @author hacksaw
 */
@Profile("test")
public class CommonTest {
    @Test
    public void urlMatchTest() {
        assertTrue(Common.urlMatch("/api/*/account", "/api/v1/account"));
        assertTrue(Common.urlMatch("/api/*/account", "/api/v2/account"));
        assertTrue(Common.urlMatch("/api/*", "/api/v1"));
        assertTrue(Common.urlMatch("/api/*", "/api/v2"));
    }
}
