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

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class UrlTransform {

  // Transform patterns.
  private static final Pattern PATTERN_FROM = Pattern.compile("\\(.*?\\|");
  private static final Pattern PATTERN_TO = Pattern.compile("\\|.*?\\)");
  private static final Pattern PATTERN_URI = Pattern.compile(
          "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

  private final String uriTransform;

  /**
   *
   * @param uriTransform
   */
  public UrlTransform(String uriTransform) {
    this.uriTransform = uriTransform;
  }

  /**
   *
   * @param uri
   * @return
   * @throws MalformedURLException
   */
  public UriComponentsBuilder getPath(String uri)
          throws MalformedURLException {
    if (uriTransform == null || uriTransform.isBlank()) {
      return UriComponentsBuilder.fromUriString(uri);
    }

    // We want to manipulate the URL using string matching.
    String fromUri = getFromURI(uriTransform);
    String toUri = getToURI(uriTransform);

    // Remove the URI prefix if one was provided.
    if (fromUri != null && !fromUri.isBlank()) {
      if (!uri.startsWith(fromUri)) {
        // We do not have a matching URI prefix so return full URL.
        return UriComponentsBuilder.fromUriString(uri);
      }

      uri = uri.replaceFirst(fromUri, "");
    }

    // Add the URI prefix if one was provided.
    if (toUri != null && !toUri.isBlank()) {
      uri = toUri + uri;
    }

    return UriComponentsBuilder.fromUriString(uri);
  }

  /**
   * Get the fromURI component from the specified transform.
   *
   * @param transform Transform from which to extract the fromURI.
   * @return
   */
  private String getFromURI(String transform) {
    return transform == null ? null : getUri(PATTERN_FROM, transform);
  }

  /**
   * Get the toURI component from the specified transform.
   *
   * @param transform Transform from which to extract the toURI.
   * @return
   */
  private String getToURI(String transform) {
    return transform == null ? null : getUri(PATTERN_TO, transform);
  }

  /**
   * Get the URI from the specified transform using the provided pattern.
   *
   * @param pattern Regex pattern to apply to transform.
   * @param transform Transform requiring URI extraction.
   * @return The extracted URI.
   */
  private String getUri(Pattern pattern, String transform) {
    if (transform == null) {
      return null;
    }
    Matcher matcher = pattern.matcher(transform);

    if (matcher.find()) {
      String uri = matcher.group().subSequence(1, matcher.group().length() - 1).toString().trim();
      Matcher matcherUri = PATTERN_URI.matcher(uri);
      if (matcherUri.find()) {
        return matcherUri.group();
      }
    }

    return null;
  }
}
