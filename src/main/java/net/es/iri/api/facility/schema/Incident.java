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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.utils.Common;
import net.es.iri.api.facility.utils.UrlTransform;

/**
 * An incident resource groups events in time and across resources.
 *
 * @author hacksaw
 */
@Slf4j
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = """
    An Incident represents a discrete occurrence, planned or unplanned, that actually or potentially affects \
    the availability, performance, integrity, or security of one or more Resources at a given Facility. It \
    serves as a high-level grouping construct for aggregating and tracking related Events over time and \
    across multiple system components.""")
public class Incident extends NamedObject {
    public static final String URL_TEMPLATE = "%s/api/v1/status/incidents/%s";

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

    @JsonProperty("resource_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to the Resources impacted by this Incident (mayImpact).",
            example = "https://example.com/api/v1/status/resources/03bdbf77-6f29-4f66-9809-7f4f77098171",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> resourceUris = new ArrayList<>();

    @JsonProperty("event_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to the Event associated with this Incident (hasEvent).",
            example = "https://example.com/api/v1/status/events/03bdbf77-6f29-4f66-9809-7f4f77098171",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> eventUris = new ArrayList<>();

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
        this.setResourceUris(transformList(transform, this.getResourceUris()));
        this.setEventUris(transformList(transform, this.getEventUris()));
    }

    /**
     * Determines if there is an overlap between this incident's time and the time
     * duration specified in the parameters from and to.
     *
     * @param from The start of the period.
     * @param to The end of the period.
     * @return True if this incident clashes with the specified period.
     */
    public boolean isConflict(String from, String to) {
        OffsetDateTime fromFilter = Optional.ofNullable(Common.parseTime(from)).orElse(OffsetDateTime.MIN);
        OffsetDateTime toFilter = Optional.ofNullable(Common.parseTime(to)).orElse(OffsetDateTime.MAX);

        // Check for overlap between the schedule and the new meeting
        return this.isConflict(fromFilter, toFilter);
    }

    /**
     * Determines if there is an overlap between this incident's time and the time
     * duration specified in the parameters from and to.
     *
     * @param from The start of the period.
     * @param to The end of the period.
     * @return True if this incident clashes with the specified period.
     */
    public boolean isConflict(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime fromFilter = Optional.ofNullable(from).orElse(OffsetDateTime.MIN);
        OffsetDateTime toFilter = Optional.ofNullable(to).orElse(OffsetDateTime.MAX);
        OffsetDateTime startTime = Optional.ofNullable(start).orElse(OffsetDateTime.MIN);
        OffsetDateTime endTime = Optional.ofNullable(end).orElse(OffsetDateTime.MAX);

        if (fromFilter.isAfter(toFilter)) {
            log.error("isConflict: throwing out fromFilter = {}, toFilter = {}.", fromFilter, toFilter);
            return false;
        }

        if (startTime.isAfter(endTime)) {
            log.error("isConflict: throwing out startTime = {}, endTime = {}.", startTime, endTime);
            return false;
        }

        // Check for overlap between the schedule and the new meeting
        return (fromFilter.isBefore(endTime) || fromFilter.isEqual(endTime)) &&
            (toFilter.isAfter(startTime) || toFilter.isEqual(startTime));
    }

    /**
     * Determines if there is an overlap between this incident's time and the instance
     * in time specified in the query parameter "time".
     *
     * @param time The time to check for overlap.
     * @return True if this incident clashes with the specified period.
     */
    public boolean isOverlap(String time) {
        if (time == null || time.isEmpty()) {
            return true;
        }

        OffsetDateTime timeFilter = Optional.ofNullable(Common.parseTime(time)).orElse(OffsetDateTime.MIN);
        return this.isOverlap(timeFilter);
    }

    /**
     * Determines if there is an overlap between this incident's time and the instance
     * in time specified in the query parameter "time".
     *
     * @param time The time to check for overlap.
     * @return True if this incident clashes with the specified period.
     */
    public boolean isOverlap(OffsetDateTime time) {
        OffsetDateTime timeFilter = Optional.ofNullable(time).orElse(OffsetDateTime.MIN);
        OffsetDateTime startTime = Optional.ofNullable(start).orElse(OffsetDateTime.MIN);
        OffsetDateTime endTime = Optional.ofNullable(end).orElse(OffsetDateTime.MAX);

        // Check for overlap between the schedule and the new meeting
        return (timeFilter.isAfter(startTime) || timeFilter.isEqual(startTime)) &&
            (timeFilter.isBefore(endTime) || timeFilter.isEqual(endTime));
    }

    /**
     * Determines if a resource identifier listed in the resources query parameter is
     * present in the resources_uris list.
     *
     * @param resources The list of resources to match.
     * @return True if this incident contains a resource from the list.
     */
    public boolean contains(List<String> resources) {
        if (resources == null || resources.isEmpty()) {
            return true;
        }
        for (String resource : resources) {
            for (String uri : this.getResourceUris()) {
                if (uri.contains("/resources/" + resource)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void setLastModified(OffsetDateTime lastModified) {
        super.setLastModified(lastModified);
    }
}
