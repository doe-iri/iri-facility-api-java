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

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.es.iri.api.facility.utils.Common;
import net.es.iri.api.facility.utils.ResourceAnnotation;
import net.es.iri.api.facility.utils.UrlTransform;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A simple class to capture basic resource meta-data.
 *
 * @author hacksaw
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Defines discovery resource for modelling meta-data.")
public class Discovery {
    @JsonProperty("id")
    @Schema(description = "Identifier associated with resource at the target link.", example = "facility")
    private String id;

    @JsonProperty("version")
    @Schema(description = "Version of the resource based on API version", example = "v1")
    private String version;

    @JsonProperty("_links")
    @Schema(description = "A link to the resources.  Target link will be in SELF relationship.")
    private List<Link> links = new ArrayList<>();

    /**
     * Create the list of discovery metadata associated with the list of methods provided.
     *
     * @param transform THe URL transform to apply.
     * @param location The requested URL location.
     * @param methods The list of methods to scan for metadata annotations.
     * @param wildcard Return only URL metadata containing this matching URL segment.
     * @return List of discovery metadata.
     * @throws MalformedURLException If the URL was badly formed.
     */
    public static List<Discovery> getDiscovery(UrlTransform transform, String location, List<Method> methods,
                                               String wildcard) throws MalformedURLException {
        List<Discovery> discovery = new ArrayList<>();
        for (Method m : methods) {
            if (m.isAnnotationPresent(ResourceAnnotation.class)) {
                ResourceAnnotation ra = m.getAnnotation(ResourceAnnotation.class);
                RequestMapping rm = m.getAnnotation(RequestMapping.class);
                if (ra == null || rm == null) {
                    continue;
                }

                // Construct the URL to this resource.
                for (String p : rm.path()) {
                    if (Common.urlMatch(wildcard, p)) {
                        // Construct the discovery entry.
                        Discovery resource = new Discovery();
                        resource.setId(ra.name());
                        resource.setVersion(ra.version());
                        location = Common.combine(location, p);
                        UriComponentsBuilder path = transform.getPath(location);
                        Link link = Link.builder()
                            .rel(Relationships.SELF)
                            .href(path.build().encode().toUriString())
                            .type(ra.type())
                            .build();
                        resource.getLinks().add(link);
                        discovery.add(resource);
                    }
                }
            }
        }

        return discovery;
    }
}
