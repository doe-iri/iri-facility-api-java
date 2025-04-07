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
package net.es.iri.api.facility.schema;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An enumeration defining the possible types of resource.
 *
 * @author hacksaw
 */
@Schema(description = "The type of resource.")
public enum ResourceType {
    @Schema(description = "The resource is a website.")
    WEBSITE("website"),

    @Schema(description = "The resource is a service.")
    SERVICE("service"),

    @Schema(description = "The resource is a compute related.")
    COMPUTE("compute"),

    @Schema(description = "The resource is a system.")
    SYSTEM("system"),

    @Schema(description = "The resource is storage related.")
    STORAGE("storage"),

    @Schema(description = "The resource is network related.")
    NETWORK("network"),

    @Schema(description = "The resource is unknown.")
    UNKNOWN("unknown");

    private final String value;

    ResourceType(String value) {
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
    private static final Map<String, ResourceType> ID_MAP = new HashMap<>();

    static {
        for (ResourceType m : values()) {
            ID_MAP.put(m.value, m);
        }
    }

    /**
     * Utility method to get a ResourceType by value.
     *
     * @param value
     * @return
     */
    @JsonCreator
    public static ResourceType fromValue(String value) {
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
}