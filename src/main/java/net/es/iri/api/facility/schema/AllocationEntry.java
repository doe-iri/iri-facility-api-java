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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 *  This class defines an IRI allocation entry.
 *
 * @author hacksaw
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Defines an allocation.")
public class AllocationEntry {
    @JsonProperty("id")
    @Schema(description ="Unique identifier for this allocation entry.", example = "98820b64-d3ac-4eff-8e74-80decd80b37c")
    private String id; // TODO: Gabor removed.

    @JsonProperty("allocation")
    @Schema(description ="How much this allocation can spend.", example = "123.45")
    private Float allocation;

    @JsonProperty("usage")
    @Schema(description = "How much this allocation has spent.", example = "123.45")
    private Float usage;

    @JsonProperty("unit")
    @Schema(description = "The unit of this allocation.", example = "bytes")
    private AllocationUnit unit;
}
