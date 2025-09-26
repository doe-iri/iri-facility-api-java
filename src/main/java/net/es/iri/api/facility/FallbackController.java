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

import java.net.URI;
import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.schema.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The FallbackController provides a catchall for operations targeting invalid REST API endpoint URL.
 *
 * @author hacksaw
 */
@Slf4j
@Hidden
@RestController
class FallbackController {

    // Catch-all for anything not matched by more specific mappings
    @RequestMapping(path = "/api/{*path}")
    public ResponseEntity<Error> fallback(@PathVariable String path) {
        log.error("[FallbackController::fallback] entering");
        net.es.iri.api.facility.schema.Error error = Error.builder()
            .type(URI.create("about:blank"))
            .status(HttpStatus.NOT_FOUND.value())
            .title(HttpStatus.NOT_FOUND.getReasonPhrase())
            .detail("No handler for " + path)
            .instance(URI.create(path))
            .build();
        error.putExtension("timestamp", OffsetDateTime.now().toString());
        log.error("[FallbackController::fallback] returning error: {}", error);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}