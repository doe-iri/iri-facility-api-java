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
package net.es.iri.api.facility.schema;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An enumeration modelling possible incident types.
 *
 * @author hacksaw
 */
@Schema(description = "The type of incident.")
public enum IncidentType {
    @Schema(description = "The incident was planned.")
    PLANNED("planned"),

    @Schema(description = "The incident was not planned.")
    UNPLANNED("unplanned"),

    @Schema(description = "The incident is caused by a system reservation event.")
    RESERVATION("reservation");

    private final String value;

    IncidentType(String value) {
        this.value = value;
    }

    /**
     * Get the string value for this enumeration value.
     *
     * @return
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    // Reverse lookup map for getting enum by ID
    private static final Map<String, IncidentType> ID_MAP = new HashMap<>();

    static {
        for (IncidentType m : values()) {
            ID_MAP.put(m.value, m);
        }
    }

    /**
     * Utility method to get a IncidentType by value.
     *
     * @param value
     * @return
     */
    @JsonCreator
    public static IncidentType fromValue(String value) {
        return ID_MAP.getOrDefault(value, null);
    }

    /**
     * Convert the enumeration to a string.
     *
     * @return The string name for the enumeration.
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Get the set of all enum named values.
     *
     * @return The set of named values.
     */
    public static Set<String> validValues() {
        return EnumSet.allOf(IncidentType.class).stream()
            .map(Enum::name)
            .collect(Collectors.toSet());
    }
}
