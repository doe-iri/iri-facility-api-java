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
 *  An enumeration of the possible AllocationUnit values.
 *
 * @author hacksaw
 */
@Schema(title="AllocationUnit",
    description = "Defines an aspect of a resource that can have an allocation. For example," +
    "  Perlmutter nodes with GPUs. For some resources at a facility, this will be 1 to 1 with the resource." +
    "  It is a way to subdivide a resource into allocatable sub-resources further.  The word" +
    "  \"capability\" is also known to users as something they need for a job to run. (eg. gpu)")
public enum AllocationUnit {
    @Schema(description = "The allocation unit is node hours.")
    NODE_HOURS("node_hours"),

    @Schema(description = "The allocation unit is bytes.")
    BYTES("bytes"),

    @Schema(description = "The allocation unit is inodes.")
    INODES("inodes");

    private final String value;

    AllocationUnit(String value) {
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
    private static final Map<String, AllocationUnit> ID_MAP = new HashMap<>();

    static {
        for (AllocationUnit m : values()) {
            ID_MAP.put(m.value, m);
        }
    }

    /**
     * Utility method to get an AllocationUnit by value.
     *
     * @param value
     * @return
     */
    @JsonCreator
    public static AllocationUnit fromValue(String value) {
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
     * Get the set of all enum-named values.
     *
     * @return The set of named values.
     */
    public static Set<String> validValues() {
        return EnumSet.allOf(StatusType.class).stream()
            .map(Enum::name)
            .collect(Collectors.toSet());
    }
}
