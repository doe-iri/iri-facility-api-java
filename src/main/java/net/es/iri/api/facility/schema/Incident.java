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

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * An incident resource groups events in time and across resources.
 *
 * @author hacksaw
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "An incident groups events in time and across resources.")
public class Incident extends NamedObject {
    public static final String URL_TEMPLATE = "/api/v1/status/incidents/%s";

    @JsonProperty("status")
    @Schema(description = "The status of the resource associated with this incident.", example = "down")
    private StatusType status;

    @JsonProperty("type")
    @Schema(description = "The type of incident.", example = "planned")
    private IncidentType type;

    @JsonProperty("start")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    @Schema(description = "The date this incident started, or is predicted to start. Format follows the ISO 8601 standard with timezone offsets.", example = "2023-10-17T11:02:31.690-00:00")
    private OffsetDateTime start;

    @JsonProperty("end")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    @Schema(description = "The date this incident ended, or is predicted to end.  Format follows the ISO 8601 standard with timezone offsets.", example = "2023-10-19T11:02:31.690-00:00")
    private OffsetDateTime end;

    @JsonProperty("resolution")
    @Schema(description = "The resolution for this incident.", example = "pending", defaultValue = "pending")
    @Builder.Default
    private ResolutionType resolution = ResolutionType.PENDING;

    // Include embedded here since including a generic embedded structure in NamedObject screws up
    // the OpenAPI documentation.
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("_embedded")
    @Schema(description = "A set of embedded objects that were requested via the 'include' query parameter.")
    private IncidentEmbedded embedded;
}
