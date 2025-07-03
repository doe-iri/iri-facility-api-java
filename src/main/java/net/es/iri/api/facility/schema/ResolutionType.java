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
 * An enumeration modelling the current state of incident resolution.
 *
 * @author hacksaw
 */
@Schema(description = "The current state of the incident resolution.")
public enum ResolutionType {
    @Schema(description = "The incident is active.")
    UNRESOLVED("unresolved"),
    @Schema(description = "The incident was cancelled.")
    CANCELLED("cancelled"),

    @Schema(description = "The incident was completed.")
    COMPLETED("completed"),

    @Schema(description = "The incident was extended.")
    EXTENDED("extended"),

    @Schema(description = "The incident is pending.")
    PENDING("pending");

    private final String value;

    ResolutionType(String value) {
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
    private static final Map<String, ResolutionType> ID_MAP = new HashMap<>();

    static {
        for (ResolutionType m : values()) {
            ID_MAP.put(m.value, m);
        }
    }

    /**
     * Utility method to get a ResolutionType by value.
     *
     * @param value
     * @return
     */
    @JsonCreator
    public static ResolutionType fromValue(String value) {
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
        return EnumSet.allOf(ResolutionType.class).stream()
            .map(Enum::name)
            .collect(Collectors.toSet());
    }
}
