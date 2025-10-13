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
 *  This class defines an IRI project allocation.
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
@Schema(description = "Defines a project's allocation for a capability (aka. repo).  This allocation is a"
    + " piece of the total allocation for the capability (eg. 5% of the total node hours of Perlmutter GPU"
    + " nodes).  A project would at least have a storage and job repos, maybe more than 1 of each.")
public class ProjectAllocation extends NamedObject {
    public static final String URL_TEMPLATE = "%s/api/v1/account/project_allocations/%s";

    @JsonProperty("entries")
    @Schema(description = "Allocation entries associated with this user allocation (hasAllocationEntry).")
    @Builder.Default
    private List<AllocationEntry> entries =  new ArrayList<>();

    @JsonProperty("project_uri")
    @Schema(description = "A hyperlink reference (URI) to the associated project (hasProject).",
        format = "uri",
        example = "https://example.com/api/v1/account/projects/03bdbf77-6f29-4f66-9809-7f4f77098171")
    private String projectUri;

    @JsonProperty("capability_uri")
    @Schema(description = "A hyperlink reference (URI) to the associated capability (hasCapability).",
        format = "uri",
        example = "https://example.com/api/v1/account/capabilities/03bdbf77-6f29-4f66-9809-7f4f77098171")
    private String capabilityUri;

    @JsonProperty("user_allocation_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "A hyperlink reference (URI) to a UserAllocation (hasUserAllocation).",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> userAllocationUris = new ArrayList<>(); // TODO: Gabor removed.

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
        this.setProjectUri(transform(transform, this.getProjectUri()));
        this.setCapabilityUri(transform(transform, this.getCapabilityUri()));
        this.setUserAllocationUris(transformList(transform, this.getUserAllocationUris()));
    }
}
