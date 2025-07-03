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
import net.es.iri.api.facility.datastore.FacilityStatusRepository;
import net.es.iri.api.facility.schema.NamedObject;

public interface Mapping {
    /**
     * Get the object class that this Resource relationship targets.
     * @param rel The relationship.
     * @return The target object class or null.
     */
    static Mapping getTargetClass(Map<String, Mapping> relationships, String rel) {
        return relationships.get(rel);
    }

    /**
     * Get the NamedObject referenced by the URL.
     *
     * @param repository The facility status data repository.
     * @param url  The URL of the NamedObject.
     * @return The target NamedObject.
     * @throws IllegalArgumentException If the URL is not correctly formatted.
     */
    public Optional<NamedObject> getNamedObject(FacilityStatusRepository repository, String url)
        throws IllegalArgumentException;
}
