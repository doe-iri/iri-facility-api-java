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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

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
}
