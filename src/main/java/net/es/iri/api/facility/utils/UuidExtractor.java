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

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility to help handle the different forms of UUID embedded in strings.
 *
 * @author hacksaw
 */
public class UuidExtractor {

    // Case-insensitive UUID matcher (RFC 4122 shape), tolerant to surrounding text
    private static final Pattern UUID_PATTERN =
        Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    /**
     * Extracts the last UUID present in each input string.
     * - For URLs, this matches the final path segment if it's a UUID.
     * - For "urn:uuid:<uuid>", it returns <uuid>.
     * - For bare UUIDs, it returns the UUID as-is.
     * Invalid or null/blank entries are skipped.
     *
     * @param inputs list of strings possibly containing UUIDs
     * @return list of normalized (lower-case) UUID strings
     */
    public static List<String> extractUuids(List<String> inputs) {
        if (inputs == null) return Collections.emptyList();

        List<String> out = new ArrayList<>(inputs.size());
        for (String raw : inputs) {
            if (raw == null) continue;

            String s = unquoteAndTrim(raw);
            if (s.isEmpty()) continue;

            Matcher m = UUID_PATTERN.matcher(s);
            String lastMatch = null;
            while (m.find()) {
                lastMatch = m.group();
            }
            if (lastMatch != null) {
                // Validate (ensures it's a real RFC-4122 shape) and normalize
                try {
                    UUID.fromString(lastMatch);
                    out.add(lastMatch.toLowerCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    // Skip invalid UUID-like strings
                }
            }
        }
        return out;
    }

    /**
     * Optional variant if you prefer UUID objects instead of strings.
     */
    public static List<UUID> extractUuidsAsUuid(List<String> inputs) {
        List<String> strings = extractUuids(inputs);
        List<UUID> out = new ArrayList<>(strings.size());
        for (String s : strings) out.add(UUID.fromString(s));
        return out;
    }

    // Removes surrounding ASCII or curly quotes and trims whitespace, including NBSPs.
    private static String unquoteAndTrim(String s) {
        if (s == null) return "";
        // Normalize common curly quotes and weird whitespace
        String t = s
            .replace('\u201C', '"')  // left double
            .replace('\u201D', '"')  // right double
            .replace('\u2018', '\'') // left single
            .replace('\u2019', '\'') // right single
            .replace('\u00A0', ' ')  // non-breaking space
            .trim();

        // Strip matching leading/trailing quotes if present
        if (t.length() >= 2) {
            char first = t.charAt(0);
            char last  = t.charAt(t.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                t = t.substring(1, t.length() - 1).trim();
            }
        }
        return t;
    }
}
