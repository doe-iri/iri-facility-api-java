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
package net.es.iri.api.facility;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.beans.IriConfig;
import net.es.iri.api.facility.beans.ServerConfig;
import net.es.iri.api.facility.datastore.FacilityDataRepository;
import net.es.iri.api.facility.schema.MediaTypes;
import net.es.iri.api.facility.openapi.OpenApiDescriptions;
import net.es.iri.api.facility.schema.Discovery;
import net.es.iri.api.facility.schema.Error;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Location;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.IncidentType;
import net.es.iri.api.facility.schema.Link;
import net.es.iri.api.facility.schema.Relationships;
import net.es.iri.api.facility.schema.ResolutionType;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.Site;
import net.es.iri.api.facility.schema.StatusType;
import net.es.iri.api.facility.utils.Common;
import net.es.iri.api.facility.utils.ResourceAnnotation;
import net.es.iri.api.facility.utils.UrlTransform;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The StatusController provides API access to the IRI Facility Status functionality.
 * This class is annotated with OpenAPI documentation to autogenerate the OpenAPI v3
 * specification.
 *
 * @author hacksaw
 */
@Slf4j
@RestController
@Tag(name = "IRI Facility Status API", description = "Integrated Research Infrastructure Facility Status API endpoint")
public class StatusController {
    // Spring application context.
    private final ApplicationContext context;

    // The data store containing facility status information.
    private final FacilityDataRepository repository;

    // Transformer to manipulate the URL path in case of mapping issues.
    private final UrlTransform utilities;

    /**
     * Constructor for bean injection.
     *
     * @param context The Spring application context.
     * @param config The application-specific configuration.
     * @param repository The JPA-like repository holding the mock data model.
     */
    public StatusController(ApplicationContext context, IriConfig config, FacilityDataRepository repository) {
        this.context = context;
        this.repository = repository;
        this.utilities = new UrlTransform(Optional.ofNullable(config.getServer())
            .map(ServerConfig::getProxy).orElse(null));
    }

    /**
     * Initialize the controller.
     */
    @PostConstruct
    public void init() throws Exception {
        log.info("[StatusController::init] initializing controller for {} - {}.",
            context.getDisplayName(), context.getApplicationName());
    }

    /**
     * Returns a list of available Facility Status API resource endpoints.
     * <p>
     * Operation: GET /api/v1/status
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get a list of facility status meta-data.",
        description = "Returns a list of available facility status URL.",
        tags = {"getMetaData"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = Discovery.class)),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                content = @Content(
                    schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                content = @Content(
                    schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                content = @Content(
                    schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)),
        })
    @RequestMapping(path = {"/api/v1/status"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getMetaData", version = "v1", type = MediaTypes.DISCOVERY)
    public ResponseEntity<?> getMetaData() {
        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getMetaData] GET operation = {}", location);

            // We will populate some HTTP response headers.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            List<Method> methods = Stream.of(StatusController.class.getMethods()).toList();
            List<Discovery> discovery = FacilityController.getDiscovery(utilities, location.toASCIIString(),  methods);

            return new ResponseEntity<>(discovery, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[StatusController::getMetaData] Exception caught", ex);
            Error error = Error.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .title(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .detail(ex.getMessage())
                .instance(location)
                .build();
            error.putExtension("timestamp", OffsetDateTime.now().toString());
            log.error("[StatusController::getMetaData] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the resources associated with this facility.
     * <p>
     * Operation: GET /api/v1/status/resources
     *            GET /api/v1/facility/resources
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all models with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param group An optional query parameter that will filter resources base
     *              on a group string.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get a collection of IRI resources.",
        description = "Returns a list of IRI resources matching the query.",
        tags = {"getResources"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = Resource.class)),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(path = {"/api/v1/status/resources", "/api/v1/facility/resources"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResources", version = "v1", type = MediaTypes.RESOURCES)
    public ResponseEntity<?> getResources(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.GROUP_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.GROUP_MSG) String group,
        @RequestParam(value = OpenApiDescriptions.RESOURCE_TYPE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.RESOURCE_TYPE_MSG) String type,
        @RequestParam(value = OpenApiDescriptions.RESOURCE_CAPABILITY_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.RESOURCE_CAPABILITY_MSG) List<String> capabilities,
        @RequestParam(value = OpenApiDescriptions.CURRENT_STATUS_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.CURRENT_STATUS_MSG) List<String> currentStatus) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getResources] GET operation = {}, accept = {}, "
                    + "If-Modified-Since = {}, group = {}, capabilities = {}",
                location, accept, ifModifiedSince, group, capabilities);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Resource> results = repository.findAllResources();

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Resource::getLastModified)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the header
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer, we return
            // them all.
            if (ifms != null) {
                log.debug("[StatusController::getResources] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since the specified time.
                    log.debug("[StatusController::getResources] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Now compute those resources that have changed since the If-Modified-Since time.
                results = results.stream()
                    .filter(r -> r.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the group filter if requested.
            if (group != null && !group.isBlank()) {
                // Filter resources from the specified group.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(group).equalsIgnoreCase(r.getGroup()))
                    .collect(Collectors.toList());
            }

            // Apply the type filter if requested.
            if (type != null && !type.isBlank()) {
                // Filter resources from the specified type.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(type).equalsIgnoreCase(r.getType().getValue()))
                    .collect(Collectors.toList());
            }

            // Apply the capability filter if requested.
            if (capabilities != null && !capabilities.isEmpty()) {
                // Normalize inputs once
                final List<String> normalizedCaps = capabilities.stream()
                    .filter(Objects::nonNull)
                    .map(Common::stripQuotes)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();

                if (!normalizedCaps.isEmpty()) {
                    results = results.stream()
                        .filter(r -> {
                            List<String> uris = r.getCapabilityUris();
                            return uris != null && !uris.isEmpty()
                                && normalizedCaps.stream().anyMatch(cap -> Common.contains(cap, uris));
                        })
                        .collect(Collectors.toList());
                }
            }


            // Apply the currentStatus filter if requested.
            if (currentStatus != null && !currentStatus.isEmpty()) {
                // Filter any filter values not in the StatusType enum.
                Set<StatusType> wanted = currentStatus.stream()
                    .map(String::toUpperCase)
                    .map(Common::stripQuotes)
                    .filter(StatusType.validValues()::contains)
                    .map(StatusType::valueOf)
                    .collect(Collectors.toSet());

                // Filter resources from the specified type.
                results = results.stream()
                    .filter(r -> wanted.contains(r.getCurrentStatus()))
                    .collect(Collectors.toList());
            }

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[StatusController::getResources] Exception caught in GET of /resources", ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the resource associated with the specified id.
     * <p>
     * Operation: GET /api/v1/status/resources/{id}
     *            GET /api/v1/facility/resources/{id}
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param id The identifier of the target resource.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get the IRI resource associated with the specified id.",
        description = "Returns the matching IRI resource.",
        tags = {"getResource"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Resource.class)
                )
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_FOUND_CODE,
                description = OpenApiDescriptions.NOT_FOUND_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/resources/{id}",
            "/api/v1/facility/resources/{id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResource", version = "v1", type = MediaTypes.RESOURCE)
    public ResponseEntity<?> getResource(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getResource] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // Find the resource targeted by id.
            Resource resource = repository.findResourceById(id);
            if (resource != null) {
                OffsetDateTime lastModified = resource.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since the specified time.
                        log.debug("[StatusController::getResource] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching resource.
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            }

            log.error("[StatusController::getResource] resource not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getResource] Exception caught in GET of /resources/{}", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns a list of "incident" resources.
     * <p>
     * Operation: GET /api/v1/status/incidents
     *            GET /api/v1/facility/incidents
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all models with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get a list of available incident resources.",
        description = "Returns a list of incident resources matching the specified query",
        tags = {"getIncidents"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = Incident.class)),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/incidents", "/api/v1/facility/incidents" },
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncidents", version = "v1", type = MediaTypes.INCIDENTS)
    public ResponseEntity<?> getIncidents(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.STATUS_TYPE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.STATUS_TYPE_MSG,
            schema = @Schema(implementation = StatusType.class)) String status,
        @RequestParam(value = OpenApiDescriptions.INCIDENT_TYPE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.INCIDENT_TYPE_MSG,
            schema = @Schema(implementation = IncidentType.class)) String type,
        @RequestParam(value = OpenApiDescriptions.RESOLUTION_TYPE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.RESOLUTION_TYPE_MSG,
            schema = @Schema(implementation = ResolutionType.class)) String resolution,
        @RequestParam(value = OpenApiDescriptions.TIME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.TIME_MSG) String time,
        @RequestParam(value = OpenApiDescriptions.FROM_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.FROM_MSG) String from,
        @RequestParam(value = OpenApiDescriptions.TO_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.TO_MSG) String to,
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName,
        @RequestParam(value = OpenApiDescriptions.RESOURCES_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.RESOURCES_MSG) List<String> resources) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getIncidents] GET operation = {}, accept = {}, "
                    + "If-Modified-Since = {}, status = {}. type = {}. resolution = {}, time = {},"
                    + "from = {}, to = {}, shortName = {}, resources = {}",
                location, accept, ifModifiedSince, status, type, resolution, time,
                from, to, shortName, resources);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Incident> results = repository.findAllIncidents();

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Incident::getLastModified)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the Last-Modified header.
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer, we return
            // them all.
            if (ifms != null) {
                log.debug("[StatusController::getIncidents] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since the specified time.
                    log.debug("[StatusController::getIncidents] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Now add resources that have changed since the If-Modified-Since time.
                results = results.stream()
                    .filter(incident -> incident.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the status filter is requested.
            if (status != null && !status.isBlank()) {
                // Filter resources from the specified group.
                results = results.stream()
                    .filter(incident -> Common.stripQuotes(status).equalsIgnoreCase(incident.getStatus().getValue()))
                    .collect(Collectors.toList());
            }

            // Apply the type filter requested.
            if (type != null && !type.isBlank()) {
                // Filter resources from the specified type.
                results = results.stream()
                    .filter(incident -> Common.stripQuotes(type).equalsIgnoreCase(incident.getType().getValue()))
                    .collect(Collectors.toList());
            }

            // Apply the type filter requested.
            if (resolution != null && !resolution.isBlank()) {
                // Filter resources from the specified type.
                results = results.stream()
                    .filter(incident -> Common.stripQuotes(resolution).equalsIgnoreCase(incident.getResolution().getValue()))
                    .collect(Collectors.toList());
            }

            // The "time" query parameter is specified to return incidents overlapping with time.
            results = results.stream()
                .filter(incident -> incident.isOverlap(time))
                .collect(Collectors.toList());

            // The "from" query parameter is the start of the time the user is looking for active incidents.
            // The "to" query parameter is the end of the time the user is looking for active incidents.
            // This means we want any incidents active before this time.
            results = results.stream()
                .filter(incident -> incident.isConflict(from, to))
                .collect(Collectors.toList());

            // Find all the remaining incidents that contain a resource from the provided list.
            results = results.stream()
                .filter(incident -> incident.contains(resources))
                .collect(Collectors.toList());

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[StatusController::getIncidents] Exception caught in GET of /incidents", ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the incident resource associated with the specified id.
     * <p>
     * Operation: GET /api/v1/status/incident/{id}
     *            GET /api/v1/facility/incident/{id}
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment, 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param id The identifier of the target incident resource.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get the incident resource associated with the specified id.",
        description = "Returns the matching incident resource.",
        tags = {"getIncident"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Incident.class)
                )
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_FOUND_CODE,
                description = OpenApiDescriptions.NOT_FOUND_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/incidents/{id}",
            "/api/v1/facility/incidents/{id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncident", version = "v1", type = MediaTypes.INCIDENT)
    public ResponseEntity<?> getIncident(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getIncidents] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // Find the incident matching the specified id.
            Incident incident = repository.findIncidentById(id);
            if (incident != null) {
                OffsetDateTime lastModified = incident.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since the specified time.
                        log.debug("[StatusController::getIncidents] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching resource.
                return new ResponseEntity<>(incident, headers, HttpStatus.OK);
            }

            log.error("[StatusController::getIncidents] incident not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getIncidents] Exception caught in GET of /incidents/{}", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the events associated with the specified incident.
     * <p>
     * Operation: GET /api/v1/status/incident/{id}/events
     *            GET /api/v1/facility/incident/{id}/events
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment, 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param id The identifier of the target incident resource.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get the events associated with the specified incident.",
        description = "RReturns the events associated with the specified incident.",
        tags = {"getEventsByIncident"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = Event.class)),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_FOUND_CODE,
                description = OpenApiDescriptions.NOT_FOUND_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/incidents/{id}/events",
            "/api/v1/facility/incidents/{id}/events"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getEventsByIncident", version = "v1", type = MediaTypes.EVENTS)
    public ResponseEntity<?> getEventsByIncident(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getEventsByIncident] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // Find the incident matching the specified id.
            Incident incident = repository.findIncidentById(id);
            if (incident != null) {
                // Look up the resource associated with the event.
                List<Event> events = new ArrayList<>();
                for (String uri : incident.getEventUris()) {
                    Event event = repository.findEventByHref(uri);
                    if (event != null) {
                        events.add(event);
                    }
                }

                // Find the latest modified timestamp among all resources
                OffsetDateTime lastModified = events.stream()
                    .map(Event::getLastModified)
                    .max(OffsetDateTime::compareTo)
                    .orElse(OffsetDateTime.now());

                // Populate the Last-Modified header.
                headers.setLastModified(lastModified.toInstant());

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since the specified time.
                        log.debug("[StatusController::getEventsByIncident] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }

                    // Now add resources that have changed since the If-Modified-Since time.
                    events = events.stream()
                        .filter(r -> r.getLastModified().isAfter(ifms))
                        .collect(Collectors.toList());
                }

                // Return the matching resource.
                return new ResponseEntity<>(events, headers, HttpStatus.OK);
            }

            log.error("[StatusController::getEventsByIncident] event not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getEventsByIncident] Exception caught in GET of /incidents/{}/events", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns a list of events.
     * <p>
     * Operation: GET /api/v1/status/events
     *            GET /api/v1/facility/events
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all models with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get a list of event resources.",
        description = "Returns a list of event resources matching the specified query",
        tags = {"getEvents"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = Event.class)),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(path = {"/api/v1/status/events", "/api/v1/facility/events"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getEvents", version = "v1", type = MediaTypes.EVENTS)
    public ResponseEntity<?> getEvents(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.STATUS_TYPE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.STATUS_TYPE_MSG,
            schema = @Schema(implementation = StatusType.class)) String status,
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName,
        @Parameter(description = OpenApiDescriptions.TIME_MSG) String time,
        @RequestParam(value = OpenApiDescriptions.FROM_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.FROM_MSG) String from,
        @RequestParam(value = OpenApiDescriptions.TO_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.TO_MSG) String to) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getEvents] GET operation = {}, accept = {}, "
                    + "If-Modified-Since = {}",
                location, accept, ifModifiedSince);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Event> results = repository.findAllEvents();

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Event::getLastModified)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the Last-Modified header.
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer, we return
            // them all.
            if (ifms != null) {
                log.debug("[StatusController::getEvents] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since the specified time.
                    log.debug("[StatusController::getEvents] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Now add resources that have changed since the If-Modified-Since time.
                results = results.stream()
                    .filter(r -> r.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the status filter is requested.
            if (status != null && !status.isBlank()) {
                // Filter resources from the specified group.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(status).equalsIgnoreCase(r.getStatus().getValue()))
                    .collect(Collectors.toList());
            }

            if (from != null && !from.isBlank()) {
                OffsetDateTime timeFilter = Common.parseTime(from);

                results = results.stream()
                    .filter(event -> {
                        OffsetDateTime occurredAt = Optional.ofNullable(event.getOccurredAt()).orElse(OffsetDateTime.MIN);
                        log.debug("[StatusController::getEvents] from = {}, occurredAt = {}",
                            from, occurredAt);
                        return (occurredAt.isAfter(timeFilter) || occurredAt.isEqual(timeFilter));
                    })
                    .collect(Collectors.toList());
            }

            if (to != null && !to.isBlank()) {
                OffsetDateTime timeFilter = Common.parseTime(to);

                results = results.stream()
                    .filter(event -> {
                        OffsetDateTime occurredAt = Optional.ofNullable(event.getOccurredAt()).orElse(OffsetDateTime.MAX);
                        log.debug("[StatusController::getEvents] from = {}, getOccurredAt = {}",
                            from, occurredAt);
                        return (occurredAt.isBefore(timeFilter) || occurredAt.isEqual(timeFilter));
                    })
                    .collect(Collectors.toList());
            }

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[StatusController::getEvents] Exception caught in GET of /events", ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     *
     * <p>
     * Operation: GET /api/v1/status/events/{id}
     *            GET /api/v1/facility/events/{id}
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param id The identifier of the target incident resource.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get the event associated with the specified id.",
        description = "Returns the event associated with the specified id.",
        tags = {"getEvent"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Event.class)
                )
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_FOUND_CODE,
                description = OpenApiDescriptions.NOT_FOUND_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/events/{id}",
            "/api/v1/facility/events/{id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getEvent", version = "v1", type = MediaTypes.EVENT)
    public ResponseEntity<?> getEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // Get the event matching the specified id.
            Event event = repository.findEventById(id);
            if (event != null) {
                OffsetDateTime lastModified = event.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since the specified time.
                        log.debug("[StatusController::getEvent] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching resource.
                return new ResponseEntity<>(event, headers, HttpStatus.OK);
            }

            log.error("[StatusController::getEvent] event not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getEvent] Exception caught in GET of /events/{}", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the resource impacted by the specified event.
     * <p>
     * Operation: GET /api/v1/status/events/{id}/resource
     *            GET /api/v1/facility/events/{id}/resource
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param id The identifier of the target incident resource.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get the resource impacted by the specified event.",
        description = "Returns the resource impacted by the specified event.",
        tags = {"getResourceByEvent"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Resource.class)
                )
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_FOUND_CODE,
                description = OpenApiDescriptions.NOT_FOUND_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/events/{id}/resource",
            "/api/v1/facility/events/{id}/resource"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResourceByEvent", version = "v1", type = MediaTypes.RESOURCE)
    public ResponseEntity<?> getResourceByEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getResourceByEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // Find the specified event, then the impacted resources.
            Event event = repository.findEventById(id);
            if (event != null) {
                // Look up the resource associated with the event.
                Resource resource = repository.findResourceByHref(event.getResourceUri());
                if (resource != null) {
                    OffsetDateTime lastModified = resource.getLastModified();
                    if (lastModified != null) {
                        // Populate the header
                        headers.setLastModified(lastModified.toInstant());
                    }

                    // Parse the If-Modified-Since header if it is present.
                    OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                    if (ifms != null && lastModified != null) {
                        if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                            // The resource has not been modified since the specified time.
                            log.debug("[StatusController::getResourceByEvent] returning NOT_MODIFIED");
                            return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                        }
                    }

                    return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                }
            }

            log.error("[StatusController::getResourceByEvent] event not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getResourceByEvent] Exception caught in GET of /events/{}/resource", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the incident related to the specified event.
     * <p>
     * Operation: GET /api/v1/status/events/{id}/incident
     *            GET /api/v1/facility/events/{id}/incident
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param id The identifier of the target incident resource.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get the incident related to the specified event.",
        description = "Returns the incident related to the specified event.",
        tags = {"getIncidentByEvent"},
        method = "GET")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = OpenApiDescriptions.OK_CODE,
                description = OpenApiDescriptions.OK_MSG,
                headers = {
                    @Header(name = HttpHeaders.LOCATION,
                        description = OpenApiDescriptions.LOCATION_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Incident.class)
                )
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_MODIFIED_CODE,
                description = OpenApiDescriptions.NOT_MODIFIED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class)),
                    @Header(name = HttpHeaders.LAST_MODIFIED,
                        description = OpenApiDescriptions.LAST_MODIFIED_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content()
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.NOT_FOUND_CODE,
                description = OpenApiDescriptions.NOT_FOUND_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.BAD_REQUEST_CODE,
                description = OpenApiDescriptions.BAD_REQUEST_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.UNAUTHORIZED_CODE,
                description = OpenApiDescriptions.UNAUTHORIZED_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                headers = {
                    @Header(name = HttpHeaders.CONTENT_TYPE,
                        description = OpenApiDescriptions.CONTENT_TYPE_DESC,
                        schema = @Schema(implementation = String.class))
                },
                content = @Content(schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/events/{id}/incident",
            "/api/v1/facility/events/{id}/incident"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncidentByEvent", version = "v1", type = MediaTypes.INCIDENT)
    public ResponseEntity<?> getIncidentByEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[StatusController::getIncidentByEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // Locate the specified event, then find the generatedBy relationship to identify Incident.
            Event event = repository.findEventById(id);
            if (event != null) {
                // Look up the resource associated with the event.
                Incident incident = repository.findIncidentByHref(event.getIncidentUri());
                if (incident != null) {
                    OffsetDateTime lastModified = incident.getLastModified();
                    if (lastModified != null) {
                        // Populate the header
                        headers.setLastModified(lastModified.toInstant());
                    }

                    // Parse the If-Modified-Since header if it is present.
                    OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                    if (ifms != null && lastModified != null) {
                        if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                            // The resource has not been modified since the specified time.
                            log.debug("[StatusController::getIncidentByEvent] returning NOT_MODIFIED");
                            return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                        }
                    }

                    return new ResponseEntity<>(incident, headers, HttpStatus.OK);
                }
            }

            log.error("[StatusController::getIncidentByEvent] incident not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getIncidentByEvent] Exception caught in GET of /events/{}/incident", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
