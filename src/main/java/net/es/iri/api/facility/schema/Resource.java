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
import java.util.Optional;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *  This class defines an IRI resource and associated dependencies.
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
    A Resource is an object that either offers a service to the end user, or supports a resource that \
    offers a service to the end user.  This resource will have an associated operational state, events, \
    and incidents.""")
public class Resource extends NamedObject {
    public static final String URL_TEMPLATE = "%s/api/v1/status/resources/%s";

    @JsonProperty("resource_type")
    @Schema(description = "The type of resource.", example = "system",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private ResourceType type;

    @JsonProperty("capability_uris")
    @ArraySchema(
        arraySchema = @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED),
        schema = @Schema(
            description = "Hyperlink references (URIs) to capabilities this resource provides (hasCapability).",
            example="https://example.com/api/v1/account/capabilities/b1ce8cd1-e8b8-4f77-b2ab-152084c70281",
            type = "string", format = "uri")
    )
    @Builder.Default
    private List<String> capabilityUris = new ArrayList<>();

    @JsonProperty("current_status")
    @Schema(description = "The current status of this resource at time of query.", example = "up",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private StatusType currentStatus;

    @JsonProperty("located_at_uri")
    @Schema(description = "A hyperlink reference (URI) to the Site containing this Resource (locatedAt).",
        format = "uri",
        example = "https://example.com/api/v1/facility/sites/ce2bbc49-ba63-4711-8f36-43b74ec2fe45",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String locatedAtUri;

    @JsonProperty("member_of_uri")
    @Schema(description = "A hyperlink reference (URI) to facility managing this Resource (memberOf).",
        format = "uri",
        example = "https://example.com/api/v1/facility",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String memberOfUri;

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
        this.setLocatedAtUri(transform(transform, this.getLocatedAtUri()));
        this.setMemberOfUri(transform(transform, this.getMemberOfUri()));
    }
}
