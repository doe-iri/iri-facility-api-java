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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Parent class containing common naming attributes for a resource.
 *
 * @author hacksaw
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Parent object definition containing common properties for use in named objects.")
public class NamedObject {
    @JsonProperty("id")
    @Schema(description = "The unique identifier for the object.  Typically a UUID or URN to provide global uniqueness across facilities.", example = "09a22593-2be8-46f6-ae54-2904b04e13a4", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @JsonProperty("name")
    @Schema(description = "The long name of the resource.", example = "Lawrence Berkeley National Laboratory")
    private String name;

    @JsonProperty("short_name")
    @Schema(description = "The short name of the resource.", example = "LBNL")
    private String shortName;

    @JsonProperty("description")
    @Schema(description = "A description of the resource.", example = "Lawrence Berkeley National Laboratory is charged with conducting unclassified research across a wide range of scientific disciplines.")
    private String description;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    @Schema(description = "The date this resource was last modified. Format follows the ISO 8601 standard with timezone offsets.", example = "2025-03-11T07:28:24.000−00:00")
    private OffsetDateTime lastModified;

    /**
     * Setter for the LastModified time.
     *
     * @param lastModified The last modified time.
     */
    public void setLastModified(OffsetDateTime lastModified) {
        // Round this down to the accuracy of the If-Modified-Header.
        this.lastModified = lastModified.truncatedTo(ChronoUnit.SECONDS);
    }

    @JsonProperty("_links")
    @Schema(description = "A list of links to other resources with defined relationships such as locatedAt, hostedAt, hasIncident, hasEvent, hasResource, hasDependent, dependsOn, impacts.")
    @Builder.Default
    private List<Link> links = new ArrayList<>();
}
