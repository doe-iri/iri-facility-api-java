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

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import net.es.iri.api.facility.utils.UrlTransform;

/**
 * Defines a resource-impacting event and provides log functionality.
 *
 * @author hacksaw
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = """
    An Event represents a discrete, timestamped occurrence that reflects a change in state, condition, \
    or behavior of a Resource, typically within the context of an ongoing Incident. Events provide \
    the fine-grained details necessary to understand the progression and impact of an Incident, \
    serving as both diagnostic data and a lightweight status log of relevant activity.""")
public class Event extends NamedObject {
    public static final String URL_TEMPLATE = "%s/api/v1/status/events/%s";

    @JsonProperty("status")
    @Schema(description = "The status of the resource associated with this event.", example = "down")
    private StatusType status;

    @JsonProperty("occurred_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    @Schema(description = "The date this event occurred.  Format follows the ISO 8601 standard with timezone offsets.",
        example = "2025-03-11T07:28:24.000−00:00")
    private OffsetDateTime occurredAt;

    @JsonProperty("resource_uri")
    @Schema(description = "A hyperlink reference (URI) to the Resource associated with this Event (impacts).",
        format = "uri",
        example = "https://example.com/api/v1/status/resources/03bdbf77-6f29-4f66-9809-7f4f77098171")
    private String resourceUri;

    @JsonProperty("incident_uri")
    @Schema(description = "A hyperlink reference (URI) to the Incident associated with this Event (generatedBy).",
        format = "uri",
        example = "https://example.com/api/v1/status/incidents/03bdbf77-6f29-4f66-9809-7f4f77098171")
    private String incidentUri;

    /**
     * Returns the URL template for use by the parent class for exposing the Self URL.
     *
     * @return The URL template for an instance of this resource.
     */
    @Override protected String getUrlTemplate() {
        return URL_TEMPLATE;
    }

    /**
     * Run the transform over all URI in the resource.
     *
     * @param transform The transform to run on the resource.
     */
    @Override
    public void transformUri(UrlTransform transform) {
        this.setSelfUri(transform(transform, this.getSelfUri()));
        this.setResourceUri(transform(transform, this.getResourceUri()));
        this.setIncidentUri(transform(transform, this.getIncidentUri()));
    }
}
