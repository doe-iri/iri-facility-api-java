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
 *  This class defines an IRI resource and associated dependencies.
 *
 * @author hacksaw
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Defines a resource that has a reportable status and its associated dependencies.")
public class Resource extends NamedObject {
    public static final String URL_TEMPLATE = "/api/v1/status/resources/%s";

    @JsonProperty("type")
    @Schema(description = "The type of resource.", example = "system")
    private ResourceType type;

    @JsonProperty("group")
    @Schema(description = "The member resource group.", example = "PERLMUTTER")
    private String group;

    @JsonProperty("current_status")
    @Schema(description = "The current status of this resource a time of query.", example = "up")
    private StatusType currentStatus;
}
