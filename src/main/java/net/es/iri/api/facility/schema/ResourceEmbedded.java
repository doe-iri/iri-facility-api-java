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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "The allowable embedded objects for Resource.")
public class ResourceEmbedded {
    @JsonProperty("memberOf")
    private List<Facility> memberOf;

    @JsonProperty("locatedAt")
    private List<Site> locatedAt;

    @JsonProperty("hasIncident")
    private List<Incident> hasIncident;

    @JsonProperty("impactedBy")
    private List<Event> impactedBy;

    @JsonProperty("dependsOn")
    private List<Resource> dependsOn;

    @JsonProperty("hasDependent")
    private List<Resource> hasDependent;

    /**
     * Determine if this instance is empty.
     *
     * @return true if all embedded lists are null or contain no elements.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return (memberOf    == null || memberOf.isEmpty())
            && (locatedAt == null || locatedAt.isEmpty())
            && (hasIncident == null || hasIncident.isEmpty())
            && (impactedBy    == null || impactedBy.isEmpty())
            && (dependsOn == null || dependsOn.isEmpty())
            && (hasDependent == null || hasDependent.isEmpty());
    }
}