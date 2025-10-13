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
 * This class models a geographical location in which resources are located.
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
    A Location models the geographic, geopolitical, or spatial context associated with a Site that may \
    contain zero or more Resources. It serves as a foundational concept for expressing where resources \
    physically exist, are operated, or are logically grouped, enabling meaningful organization and \
    visualization.  Locations are reusable entities that may be shared across multiple Sites or \
    Facilities.""")
public class Location extends NamedObject {
    public static final String URL_TEMPLATE = "%s/api/v1/facility/locations/%s";

    @JsonProperty("short_name")
    @Schema(description = "The short name of the resource.", example = "LBNL")
    private String shortName;

    @JsonProperty("country_name")
    @Schema(description = "The country name of the location.", example = "United States of America")
    private String countryName;

    @JsonProperty("locality_name")
    @Schema(description = "The city or locality name of the location.", example = "Berkeley")
    private String localityName;

    @JsonProperty("state_or_province_name")
    @Schema(description = "The state or province name of the location.", example = "California")
    private String stateOrProvinceName;

    @JsonProperty("street_address")
    @Schema(description = "The street address of the location.", example = "1 Cyclotron Rd.")
    private String streetAddress;

    @JsonProperty("unlocode")
    @Schema(description = "The United Nations code for trade and transport locations.", example = "US JBK")
    private String unlocode;

    @JsonProperty("altitude")
    @Schema(description = "The altitude of the location.", example = "240")
    private double altitude;

    @JsonProperty("latitude")
    @Schema(description = "The latitude of the location.", example = "37.87492")
    private double latitude;

    @JsonProperty("longitude")
    @Schema(description = "The longitude of the location.", example = "-122.2529")
    private double longitude;

    @JsonProperty("site_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a Site located at this Location (hasSite).",
            example = "https://example.com/api/v1/status/sites/03bdbf77-6f29-4f66-9809-7f4f77098171",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> siteUris =  new ArrayList<>();

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
        this.setSiteUris(transformList(transform, this.getSiteUris()));
    }
}
