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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * This class models a geographical location in which resources are located.
 *
 * @author hacksaw
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "These attribute are concerned with geographical positions or regions with which resources are associated.")
public class GeographicalLocation extends NamedObject {
    public static final String URL_TEMPLATE = "/api/v1/status/locations/%s";

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
}
