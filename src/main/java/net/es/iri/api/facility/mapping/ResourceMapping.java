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

package net.es.iri.api.facility.mapping;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.es.iri.api.facility.datastore.FacilityStatusRepository;
import net.es.iri.api.facility.schema.NamedObject;

public class ResourceMapping implements Mapping {
    // Map relationships to the target object class.
    public static final Map<String, Mapping> RELATIONSHIP_TARGETS = Map.of(
        Relationships.MEMBER_OF, new FacilityMapping(),
        Relationships.LOCATED_AT, new SiteMapping(),
        Relationships.HAS_INCIDENT, new IncidentMapping(),
        Relationships.IMPACTED_BY, new EventMapping(),
        Relationships.DEPENDS_ON, new ResourceMapping(),
        Relationships.HAS_DEPENDENT, new ResourceMapping());

    /**
     * Get the NamedObject referenced by the URL.
     *
     * @param repository The facility status data repository.
     * @param url  The URL of the NamedObject.
     * @return The target NamedObject.
     * @throws IllegalArgumentException If the URL is not correctly formatted.
     */
    @Override
    public Optional<NamedObject> getNamedObject(FacilityStatusRepository repository, String url)
            throws IllegalArgumentException {
        UUID uuid = extractId(url);
        return repository.findById(uuid.toString());
    }

    // this regex looks for “/resources/” followed by a 36-char UUID and anchors at end-of-string.
    private static final Pattern PATTERN_UUID =
        Pattern.compile("/resources/([0-9a-fA-F\\-]{36})$");

    /**
     * Extracts the UUID from any string containing “/resources/{uuid}” at the end.
     *
     * @param url the full path or URL
     * @return the UUID that followed “/resources/”
     * @throws IllegalArgumentException if no “/resources/{uuid}” is found or UUID is malformed
     */
    private static UUID extractId(String url) throws IllegalArgumentException {
        Matcher m = PATTERN_UUID.matcher(url);
        if (m.find()) {
            return UUID.fromString(m.group(1));
        }
        throw new IllegalArgumentException("Invalid path (must end with /resources/{uuid}): " + url);
    }
}
