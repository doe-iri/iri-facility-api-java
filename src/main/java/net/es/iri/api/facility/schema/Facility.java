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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
import net.es.iri.api.facility.utils.UrlTransform;

/**
 * A DoE laboratory or production facility that offers resources for programmatic consumption.
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
    A specialized research facility funded and operated by the U.S. Department of Energy that provides \
    unique scientific tools, expertise, and infrastructure for researchers from across academia, industry, \
    and government.  In a more general definition, a Facility offers Resources to scientific workflows for \
    programmatic consumption.""")
public class Facility extends NamedObject {
    public static final String URL_TEMPLATE = "%s/api/v1/facility/%s";

    @JsonProperty("short_name")
    @Schema(description = "The short name of the resource.", example = "LBNL")
    private String shortName;

    @JsonProperty("organization_name")
    @Schema(description = "The name of the organization hosting the facility.",
        example = "Lawrence Berkeley National Laboratory")
    private String organizationName;

    @JsonProperty("support_uri")
    @Schema(description = "A hyperlink reference (URI) to the support website for this Facility (supportURL).",
        format = "uri",
        example = "https://help.nersc.gov/")
    private String supportUri;

    @JsonProperty("site_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a Site.  A Facility can be hosted at zero or more physical Sites (hostedAt).",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> siteUris = new ArrayList<>();

    @JsonProperty("location_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a Locations.  A Facility can be associated with zero or more geographical Locations (hasLocation).",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> locationUris = new ArrayList<>();

    @JsonProperty("resource_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a Resources contained within this Facility (hasResource).",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> resourceUris = new ArrayList<>();

    @JsonProperty("event_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to an Events that has occurred within this Facility (hasEvent).",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> eventUris = new ArrayList<>();

    @JsonProperty("incident_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to an Incidents that has occurred, or may occur within this Facility (hasIncident).",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> incidentUris = new ArrayList<>();

    @JsonProperty("capability_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a Capability associated with allocations.",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> capabilityUris = new ArrayList<>();

    @JsonProperty("project_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a Project associated with allocations.",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> projectUris = new ArrayList<>();

    @JsonProperty("project_allocation_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a Project Allocation.",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> projectAllocationUris = new ArrayList<>();

    @JsonProperty("user_allocation_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a User Allocation.",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> userAllocationUris = new ArrayList<>();

    /**
     * Returns the URL template for use by the parent class for exposing the Self URL.
     *
     * @return The URL template for an instance of this resource.
     */
    @Override
    protected String getUrlTemplate() {
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
        this.setSiteUris(transformList(transform, this.getSiteUris()));
        this.setLocationUris(transformList(transform, this.getLocationUris()));
        this.setResourceUris(transformList(transform, this.getResourceUris()));
        this.setEventUris(transformList(transform, this.getEventUris()));
        this.setIncidentUris(transformList(transform, this.getIncidentUris()));
        this.setCapabilityUris(transformList(transform, this.getCapabilityUris()));
        this.setProjectUris(transformList(transform, this.getProjectUris()));
        this.setProjectAllocationUris(transformList(transform, this.getProjectAllocationUris()));
        this.setUserAllocationUris(transformList(transform, this.getUserAllocationUris()));
    }
}
