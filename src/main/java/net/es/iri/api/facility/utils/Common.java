/*
 * IRI Facility API reference implementation Copyright (c) 2025,
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

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import net.es.iri.api.facility.schema.Error;

/**
 * Contains common HTTP message manipulation methods for controllers.
 *
 * @author hacksaw
 */
public class Common {
  /**
   * Parse the ifModifiedSince HTTP header in RFC 1123 date format into a OffsetDateTime.
   *
   * @param ifModifiedSince The ifModifiedSince header string.
   * @return The time value of ifModifiedSince as an OffsetDateTime.
   */
  public static OffsetDateTime parseIfModifiedSince(String ifModifiedSince) {
    if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
      // Parse the string into OffsetDateTime
      return OffsetDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);
    }
    return null;
  }

  public static OffsetDateTime parseTime(String time) {
    if (time != null && !time.isEmpty()) {
      // Parse the string into OffsetDateTime
      return OffsetDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
    }
    return null;
  }

  public static String stripQuotes(String input) {
    if (input == null || input.length() < 2) {
      return input;
    }
    if ((input.startsWith("\"") && input.endsWith("\"")) ||
        (input.startsWith("'") && input.endsWith("'"))) {
      return input.substring(1, input.length() - 1);
    }
    return input;
  }

  public static boolean contains(String input, List<String> uris) {
    if (input == null || input.isEmpty() || uris == null || uris.isEmpty()) {
      return false;
    }

    for (String uri : uris) {
      if (uri != null && uri.equalsIgnoreCase(input)) {
        return true;
      }
    }
    return false;
  }

  public static Error notFoundError(URI location) {
    Error error = Error.builder()
        .type(URI.create("about:blank"))
        .status(HttpStatus.NOT_FOUND.value())
        .title(HttpStatus.NOT_FOUND.getReasonPhrase())
        .detail("The resource " + location + " was not found.")
        .instance(location)
        .build();
    error.putExtension("timestamp", OffsetDateTime.now().toString());
    return error;
  }

  public static Error badRequestError(URI location, String reason) {
    Error error = Error.builder()
        .type(URI.create("about:blank"))
        .status(HttpStatus.BAD_REQUEST.value())
        .title(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .detail("The request is invalid: " + reason)
        .instance(location)
        .build();
    error.putExtension("timestamp", OffsetDateTime.now().toString());
    return error;
  }

  public static Error internalServerError(URI location, Exception ex) {
    Error error = Error.builder()
        .type(URI.create("about:blank"))
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .title(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .detail(ex.getMessage())
        .instance(location)
        .build();
    error.putExtension("timestamp", OffsetDateTime.now().toString());
    return error;
  }
}
