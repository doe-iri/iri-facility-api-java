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
 *  The physical site of a resource that has an associated location and an operating organization.
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
    A Site represents the physical and administrative context in which a Resource is deployed and operated. \
    It is associated with a geographic or network location where one or more resources reside and serves as \
    the anchor point for associating these resources with their broader infrastructure and organizational \
    relationships.
    
    The location of a resource that has an associated physical location and an operating organization.""")
public class Site extends NamedObject {
    public static final String URL_TEMPLATE = "/api/v1/facility/sites/%s";

    @JsonProperty("short_name")
    @Schema(description = "The short name of the resource.", example = "LBNL")
    private String shortName;

    @JsonProperty("operating_organization")
    @Schema(description = "The name of the organization operating the site.", example = "Lawrence Berkeley National Laboratory")
    private String operatingOrganization;

    @JsonProperty("location_uri")
    @Schema(description = "A hyperlink reference (URI) to the location associated with this Site (hasLocation).",
        format = "uri",
        example = "https://example.com/api/v1/status/locations/03bdbf77-6f29-4f66-9809-7f4f77098171")
    private String locationUri;

    @JsonProperty("resource_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a Resource located at this Site (hasResource).",
            example = "https://example.com/api/v1/status/resources/12345f77-6f29-4f66-9809-7f4f77098333",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> resourceUris = new ArrayList<>();

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
        this.setLocationUri(transform(transform, this.getLocationUri()));
        this.setResourceUris(transformList(transform, this.getResourceUris()));
    }
}
