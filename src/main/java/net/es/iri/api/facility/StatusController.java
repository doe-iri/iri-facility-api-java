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
import java.util.Comparator;
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
import net.es.iri.api.facility.datastore.FacilityDataRepository;
import net.es.iri.api.facility.openapi.OpenApiDescriptions;
import net.es.iri.api.facility.schema.Discovery;
import net.es.iri.api.facility.schema.Error;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.IncidentType;
import net.es.iri.api.facility.schema.MediaTypes;
import net.es.iri.api.facility.schema.NamedObject;
import net.es.iri.api.facility.schema.ResolutionType;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.StatusType;
import net.es.iri.api.facility.utils.Common;
import net.es.iri.api.facility.utils.ResourceAnnotation;
import net.es.iri.api.facility.utils.UrlTransform;
import org.springframework.context.ApplicationContext;
import org.springframework.format.annotation.DateTimeFormat;
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

/**
 * The StatusController provides API access to the IRI Facility Status functionality.
 * This class is annotated with OpenAPI documentation to autogenerate the OpenAPI v3
 * specification.
 *
 * @author hacksaw
 */
@Slf4j
@RestController
@Tag(name = "IRI Status API",
    description = """
        The Status API offers users programmatic access to information on the operational status \
        of various resources within a facility, scheduled maintenance/outage events, and a limited historical \
        record of these events. Designed for quick and efficient status checks of current and future \
        (scheduled) operational status, this API allows developers to integrate facility information into \
        applications or workflows, providing a streamlined way to programmatically access data without manual \
        intervention.
        
        It should be noted that the operational status of a resource is not an indication of a commitment to \
        provide service, only that the resource is in the described operational state.
        
        The Facility Status API is not intended for reporting or monitoring purposes; it does not support \
        asynchronous logging or alerting capabilities, and should not be used to derive any type of up or \
        downtime metrics. Instead, its primary focus is on delivering simple, on-demand access to facility \
        resource status, and scheduled maintenance events.""")
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

        if (config.getServer() != null) {
            String root = Optional.ofNullable(config.getServer().getRoot()).orElse("/");
            String proxy = Optional.ofNullable(config.getServer().getProxy()).orElse(config.getServer().getRoot());
            this.utilities = new UrlTransform("(" + root + "|" + proxy + ")");
            log.debug("[StatusController] root = {}, proxy = {}, urlTransform = {}",
                config.getServer().getRoot(), config.getServer().getProxy(), this.utilities.getUriTransform());
        } else {
            this.utilities = new UrlTransform(null);
        }
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            ),
            @ApiResponse(
                responseCode = OpenApiDescriptions.FORBIDDEN_CODE,
                description = OpenApiDescriptions.FORBIDDEN_MSG,
                content = @Content(
                    schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(
                responseCode = OpenApiDescriptions.INTERNAL_ERROR_CODE,
                description = OpenApiDescriptions.INTERNAL_ERROR_MSG,
                content = @Content(
                    schema = @Schema(implementation = Error.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
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
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            List<Method> methods = Stream.of(StatusController.class.getMethods()).toList();
            List<Discovery> discovery = Discovery.getDiscovery(utilities, location.toASCIIString(),
                methods, "/api/*/status");

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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            )
        })
    @RequestMapping(path = {"/api/v1/status/resources"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResources", version = "v1", type = MediaTypes.RESOURCES)
    public ResponseEntity<?> getResources(
        @RequestHeader(value = OpenApiDescriptions.ACCEPT_NAME, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = OpenApiDescriptions.IF_MODIFIED_SINCE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @RequestParam(value = OpenApiDescriptions.NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.NAME_MSG) String name,
        @RequestParam(value = OpenApiDescriptions.OFFSET_NAME, required = false, defaultValue = "0")
        @Parameter(description = OpenApiDescriptions.OFFSET_MSG,
            schema = @Schema(type = "integer", defaultValue = "0")) Integer offset,
        @RequestParam(value = OpenApiDescriptions.LIMIT_NAME, required = false, defaultValue = "100")
        @Parameter(description = OpenApiDescriptions.LIMIT_MSG,
            schema = @Schema(type = "integer", defaultValue = "100")) Integer limit,
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

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getResources] GET operation = {}, Accept = {}, " +
                    "If-Modified-Since = {}, modifiedSince = {}, name = {}, offset = {}, limit = {}, " +
                    "group = {}, type = {}, capabilities = {}, currentStatus = {}",
                location, accept, ifModifiedSince, modifiedSince, name, offset, limit,
                group, type, capabilities, currentStatus);

            // Favor the query parameter if provided.
            final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

            // We will collect the matching resources in this list.
            List<Resource> results = repository.findAllResources();

            // Find the latest modified timestamp among all resources.
            Optional<OffsetDateTime> latestModified = Common.mostRecentTimestamp(results);

            // Populate the header
            latestModified.ifPresent(l -> 
                headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer, we return
            // them all.
            if (Common.notModified(ifms, latestModified.orElse(null))) {
                // The resource has not been modified since the specified time.
                log.debug("[StatusController::getResources] returning NOT_MODIFIED");
                return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
            }

            // Now compute those resources that have changed since the If-Modified-Since time.
            if (ifms != null) {
                results = results.stream()
                    .filter(r -> r.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the name filter if requested.
            if (name != null && !name.isBlank()) {
                // Filter resources that match the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(name).equalsIgnoreCase(r.getName()))
                    .collect(Collectors.toList());
            }

            // Resource specific attributes below.

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

            // Lastly we apply any requested paging by first sorting these results and
            // then processing the offset and limit.
            results = results.stream()
                .sorted(Comparator.comparing(NamedObject::getId))
                .skip((offset == null ? 0 : Math.max(0, offset)))
                .limit((limit == null ? 100 : Math.max(0, limit)))
                .collect(Collectors.toList());

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[StatusController::getResources] Exception caught in GET of /resources", ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the resource associated with the specified id.
     * <p>
     * Operation: GET /api/v1/status/resources/{resource_id}
     *            GET /api/v1/facility/resources/{resource_id}
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param rid The resource_id of the target resource.
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/resources/{resource_id}", "/api/v1/facility/resources/{resource_id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResource", version = "v1", type = MediaTypes.RESOURCE)
    public ResponseEntity<?> getResource(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @PathVariable(OpenApiDescriptions.RID_NAME)
        @Parameter(description = OpenApiDescriptions.RID_NAME, required = true) String rid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getResource] GET operation = {}, accept = {}, "
                    + "If-Modified-Since = {}, modifiedSince = {}, rid = {}",
                location, accept, ifModifiedSince, modifiedSince, rid);

            // Find the resource targeted by id.
            Resource resource = repository.findResourceById(rid);
            if (resource != null) {
                Optional<OffsetDateTime> lastModified = Optional.ofNullable(resource.getLastModified());
                lastModified.ifPresent(l -> 
                    headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                // Favor the query parameter if provided.
                final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

                // If the request contained an If-Modified-Since header we check the entire
                // list of resources against the specified date.  If one is newer, we return
                // them all.
                if (Common.notModified(ifms, lastModified.orElse(null))) {
                  // The resource has not been modified since the specified time.
                  log.debug("[StatusController::getResource] returning NOT_MODIFIED");
                  return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Return the matching resource.
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            }

            log.error("[StatusController::getResource] resource not found {}", location);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getResource] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/incidents", "/api/v1/facility/incidents"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncidents", version = "v1", type = MediaTypes.INCIDENTS)
    public ResponseEntity<?> getIncidents(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @RequestParam(value = OpenApiDescriptions.NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.NAME_MSG) String name,
        @RequestParam(value = OpenApiDescriptions.OFFSET_NAME, required = false, defaultValue = "0")
        @Parameter(description = OpenApiDescriptions.OFFSET_MSG,
            schema = @Schema(type = "integer", defaultValue = "0")) Integer offset,
        @RequestParam(value = OpenApiDescriptions.LIMIT_NAME, required = false, defaultValue = "100")
        @Parameter(description = OpenApiDescriptions.LIMIT_MSG,
            schema = @Schema(type = "integer", defaultValue = "100")) Integer limit,
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
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.TIME_MSG) OffsetDateTime time,
        @RequestParam(value = OpenApiDescriptions.FROM_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.FROM_MSG) OffsetDateTime from,
        @RequestParam(value = OpenApiDescriptions.TO_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.TO_MSG) OffsetDateTime to,
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName,
        @RequestParam(value = OpenApiDescriptions.RESOURCES_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.RESOURCES_MSG) List<String> resources) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getIncidents] GET operation = {}, accept = {}, " +
                    "If-Modified-Since = {}, modifiedSince = {}, name = {}, offset = {}, limit = {}, " +
                    "status = {}. type = {}. resolution = {}, time = {}, from = {}, to = {}, shortName = {}, " +
                    "resources = {}",
                location, accept, ifModifiedSince, modifiedSince, name, offset, limit, status, type,
                resolution, time, from, to, shortName, resources);

            // Favor the query parameter if provided.
            final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

            // We will collect the matching resources in this list.
            List<Incident> results = repository.findAllIncidents();

            // Find the latest modified timestamp among all resources.
            Optional<OffsetDateTime> latestModified = Common.mostRecentTimestamp(results);

            // Populate the header
            latestModified.ifPresent(l -> 
                headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer, we return
            // them all.
            if (Common.notModified(ifms, latestModified.orElse(null))) {
                // The resource has not been modified since the specified time.
                log.debug("[StatusController::getIncident] returning NOT_MODIFIED");
                return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
            }

            // Now compute those resources that have changed since the If-Modified-Since time.
            if (ifms != null) {
                results = results.stream()
                    .filter(r -> r.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the name filter if requested.
            if (name != null && !name.isBlank()) {
                // Filter resources with the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(name).equalsIgnoreCase(r.getName()))
                    .collect(Collectors.toList());
            }

            // Apply the status filter is requested.
            if (status != null && !status.isBlank()) {
                // Filter resources from the specified group.
                results = results.stream()
                    .filter(incident -> Common.stripQuotes(status)
                        .equalsIgnoreCase(incident.getStatus().getValue()))
                    .collect(Collectors.toList());
            }

            // Apply the type filter requested.
            if (type != null && !type.isBlank()) {
                // Filter resources from the specified type.
                results = results.stream()
                    .filter(incident -> Common.stripQuotes(type)
                        .equalsIgnoreCase(incident.getType().getValue()))
                    .collect(Collectors.toList());
            }

            // Apply the type filter requested.
            if (resolution != null && !resolution.isBlank()) {
                // Filter resources from the specified type.
                results = results.stream()
                    .filter(incident -> Common.stripQuotes(resolution)
                        .equalsIgnoreCase(incident.getResolution().getValue()))
                    .collect(Collectors.toList());
            }

            // The "time" query parameter is specified to return incidents overlapping with time.
            if (time != null) {
                results = results.stream()
                    .filter(incident -> incident.isOverlap(time))
                    .collect(Collectors.toList());
            }

            // The "from" query parameter is the start of the time the user is looking for active incidents.
            // The "to" query parameter is the end of the time the user is looking for active incidents.
            // This means we want any incidents active before this time.
            if (from != null || to != null) {
                results = results.stream()
                    .filter(incident -> incident.isConflict(from, to))
                    .collect(Collectors.toList());
            }

            // Find all the remaining incidents that contain a resource from the provided list.
            if (resources != null && !resources.isEmpty()) {
                results = results.stream()
                    .filter(incident -> incident.contains(resources))
                    .collect(Collectors.toList());
            }

            // Lastly we apply any requested paging by first sorting these results and
            // then processing the offset and limit.
            results = results.stream()
                .sorted(Comparator.comparing(NamedObject::getId))
                .skip((offset == null ? 0 : Math.max(0, offset)))
                .limit((limit == null ? 100 : Math.max(0, limit)))
                .collect(Collectors.toList());

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[StatusController::getIncidents] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the incident resource associated with the specified id.
     * <p>
     * Operation: GET /api/v1/status/incident/{incident_id}
     *            GET /api/v1/facility/incident/{incident_id}
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment, 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param iid The incident_id of the target incident resource.
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/incidents/{incident_id}", "/api/v1/facility/incidents/{incident_id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncident", version = "v1", type = MediaTypes.INCIDENT)
    public ResponseEntity<?> getIncident(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @PathVariable(OpenApiDescriptions.IID_NAME)
        @Parameter(description = OpenApiDescriptions.IID_NAME, required = true) String iid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getIncidents] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, modifiedSince = {}, iid = {}",
                location, accept, ifModifiedSince, modifiedSince, iid);

            // Find the incident matching the specified id.
            Incident incident = repository.findIncidentById(iid);
            if (incident != null) {
                Optional<OffsetDateTime> lastModified = Optional.ofNullable(incident.getLastModified());
                lastModified.ifPresent(l -> 
                    headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                // Favor the query parameter if provided.
                final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

                // If the request contained an If-Modified-Since header we check the entire
                // list of resources against the specified date.  If one is newer, we return
                // them all.
                if (Common.notModified(ifms, lastModified.orElse(null))) {
                    // The resource has not been modified since the specified time.
                    log.debug("[StatusController::getIncidents] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Return the matching resource.
                return new ResponseEntity<>(incident, headers, HttpStatus.OK);
            }

            log.error("[StatusController::getIncidents] incident not found {}", location);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getIncidents] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the events associated with the specified incident.
     * <p>
     * Operation: GET /api/v1/status/incident/{incident_id}/events
     *            GET /api/v1/facility/incident/{incident_id}/events
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment, 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param iid The incident_id of the target incident resource.
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/incidents/{incident_id}/events",
            "/api/v1/facility/incidents/{incident_id}/events"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getEventsByIncident", version = "v1", type = MediaTypes.EVENTS)
    public ResponseEntity<?> getEventsByIncident(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @RequestParam(value = OpenApiDescriptions.NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.NAME_MSG) String name,
        @RequestParam(value = OpenApiDescriptions.OFFSET_NAME, required = false, defaultValue = "0")
        @Parameter(description = OpenApiDescriptions.OFFSET_MSG,
            schema = @Schema(type = "integer", defaultValue = "0")) Integer offset,
        @RequestParam(value = OpenApiDescriptions.LIMIT_NAME, required = false, defaultValue = "100")
        @Parameter(description = OpenApiDescriptions.LIMIT_MSG,
            schema = @Schema(type = "integer", defaultValue = "100")) Integer limit,
        @PathVariable(OpenApiDescriptions.IID_NAME)
        @Parameter(description = OpenApiDescriptions.IID_NAME, required = true) String iid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getEventsByIncident] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, modifiedSince = {}, name = {}, offset = {}, limit = {}, iid = {}",
                location, accept, ifModifiedSince, modifiedSince, name, offset, limit, iid);

            // Favor the query parameter if provided.
            final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

            // Find the incident matching the specified id.
            Incident incident = repository.findIncidentById(iid);
            if (incident != null) {
                // Look up the resource associated with the event.
                List<Event> events = new ArrayList<>();
                for (String uri : incident.getEventUris()) {
                    Event event = repository.findEventByHref(uri);
                    if (event != null) {
                        events.add(event);
                    }
                }

                // Find the latest modified timestamp among all resources.
                Optional<OffsetDateTime> latestModified = Common.mostRecentTimestamp(events);

                // Populate the header
                latestModified.ifPresent(l -> 
                    headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                // If the request contained an If-Modified-Since header we check the entire
                // list of resources against the specified date.  If one is newer, we return
                // them all.
                if (Common.notModified(ifms, latestModified.orElse(null))) {
                    // The resource has not been modified since the specified time.
                    log.debug("[StatusController::getEventsByIncident] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Now compute those resources that have changed since the If-Modified-Since time.
                if (ifms != null) {
                    events = events.stream()
                        .filter(r -> r.getLastModified().isAfter(ifms))
                        .collect(Collectors.toList());
                }

                // Return the matching resource.
                return new ResponseEntity<>(events, headers, HttpStatus.OK);
            }

            log.error("[StatusController::getEventsByIncident] event not found {}", location);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getEventsByIncident] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @RequestParam(value = OpenApiDescriptions.NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.NAME_MSG) String name,
        @RequestParam(value = OpenApiDescriptions.OFFSET_NAME, required = false, defaultValue = "0")
        @Parameter(description = OpenApiDescriptions.OFFSET_MSG,
            schema = @Schema(type = "integer", defaultValue = "0")) Integer offset,
        @RequestParam(value = OpenApiDescriptions.LIMIT_NAME, required = false, defaultValue = "100")
        @Parameter(description = OpenApiDescriptions.LIMIT_MSG,
            schema = @Schema(type = "integer", defaultValue = "100")) Integer limit,
        @RequestParam(value = OpenApiDescriptions.STATUS_TYPE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.STATUS_TYPE_MSG,
            schema = @Schema(implementation = StatusType.class)) String status,
        @RequestParam(value = OpenApiDescriptions.FROM_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.FROM_MSG) String from,
        @RequestParam(value = OpenApiDescriptions.TO_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.TO_MSG) String to) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getEvents] GET operation = {}, accept = {}, " +
                    "If-Modified-Since = {}, modifiedSince = {}, name = {}, offset = {}, limit = {}, " +
                    "status = {}, from = {}, to = {}",
                location, accept, ifModifiedSince, modifiedSince, name, offset, limit, status, from, to);

            // Favor the query parameter if provided.
            final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

            // We will collect the matching resources in this list.
            List<Event> results = repository.findAllEvents();

            // Find the latest modified timestamp among all resources.
            Optional<OffsetDateTime> latestModified = Common.mostRecentTimestamp(results);

            // Populate the header
            latestModified.ifPresent(l -> headers.setLastModified(l.toInstant()));

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer, we return
            // them all.
            if (Common.notModified(ifms, latestModified.orElse(null))) {
                // The resource has not been modified since the specified time.
                log.debug("[AccountController::getUserAllocationsByProjectAllocation] returning NOT_MODIFIED");
                return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
            }

            // Now compute those resources that have changed since the If-Modified-Since time.
            if (ifms != null) {
                results = results.stream()
                    .filter(r -> r.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the name filter if requested.
            if (name != null && !name.isBlank()) {
                // Filter resources with the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(name).equalsIgnoreCase(r.getName()))
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
                        OffsetDateTime occurredAt = Optional.ofNullable(event.getOccurredAt())
                            .orElse(OffsetDateTime.MIN);
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
                        OffsetDateTime occurredAt = Optional.ofNullable(event.getOccurredAt())
                            .orElse(OffsetDateTime.MAX);
                        log.debug("[StatusController::getEvents] from = {}, getOccurredAt = {}",
                            from, occurredAt);
                        return (occurredAt.isBefore(timeFilter) || occurredAt.isEqual(timeFilter));
                    })
                    .collect(Collectors.toList());
            }

            // Lastly we apply any requested paging by first sorting these results and
            // then processing the offset and limit.
            results = results.stream()
                .sorted(Comparator.comparing(NamedObject::getId))
                .skip((offset == null ? 0 : Math.max(0, offset)))
                .limit((limit == null ? 100 : Math.max(0, limit)))
                .collect(Collectors.toList());

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[StatusController::getEvents] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     *
     * <p>
     * Operation: GET /api/v1/status/events/{event_id}
     *            GET /api/v1/facility/events/{event_id}
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param eid The event_id of the target event resource.
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/events/{event_id}", "/api/v1/facility/events/{event_id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getEvent", version = "v1", type = MediaTypes.EVENT)
    public ResponseEntity<?> getEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @PathVariable(OpenApiDescriptions.EID_NAME)
        @Parameter(description = OpenApiDescriptions.EID_NAME, required = true) String eid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, modifiedSince = {}, id = {}",
                location, accept, ifModifiedSince, modifiedSince, eid);

            // Get the event matching the specified id.
            Event event = repository.findEventById(eid);
            if (event != null) {
                Optional<OffsetDateTime> lastModified = Optional.ofNullable(event.getLastModified());
                lastModified.ifPresent(l -> 
                    headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                // Favor the query parameter if provided.
                final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

                // If the request contained an If-Modified-Since header we check the entire
                // list of resources against the specified date.  If one is newer, we return
                // them all.
                if (Common.notModified(ifms, lastModified.orElse(null))) {
                    // The resource has not been modified since the specified time.
                    log.debug("[StatusController::getEvent] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Return the matching resource.
                return new ResponseEntity<>(event, headers, HttpStatus.OK);
            }

            log.error("[StatusController::getEvent] event not found {}", location);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getEvent] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the resource impacted by the specified event.
     * <p>
     * Operation: GET /api/v1/status/events/{event_id}/resource
     *            GET "/api/v1/facility/events/{event_id}/resource"
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param eid The event_id of the target event resource.
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/events/{event_id}/resource", "/api/v1/facility/events/{event_id}/resource"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResourceByEvent", version = "v1", type = MediaTypes.RESOURCE)
    public ResponseEntity<?> getResourceByEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @PathVariable(OpenApiDescriptions.EID_NAME)
        @Parameter(description = OpenApiDescriptions.EID_NAME, required = true) String eid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getResourceByEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, eid);

            // Find the specified event, then the impacted resources.
            Event event = repository.findEventById(eid);
            if (event != null) {
                // Look up the resource associated with the event.
                Resource resource = repository.findResourceByHref(event.getResourceUri());
                if (resource != null) {
                    Optional<OffsetDateTime> lastModified = Optional.ofNullable(resource.getLastModified());
                    lastModified.ifPresent(l -> 
                        headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                    // Favor the query parameter if provided.
                    final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

                    // If the request contained an If-Modified-Since header we check the entire
                    // list of resources against the specified date.  If one is newer, we return
                    // them all.
                    if (Common.notModified(ifms, lastModified.orElse(null))) {
                        // The resource has not been modified since the specified time.
                        log.debug("[StatusController::getResourceByEvent] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }

                    // Return the matching resource.
                    return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                }
            }

            log.error("[StatusController::getResourceByEvent] event not found {}", location);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getResourceByEvent] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the incident related to the specified event.
     * <p>
     * Operation: GET /api/v1/status/events/{event_id}/incident
     *            GET /api/v1/facility/events/{event_id}/incident
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param eid The event_id of the target incident resource.
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
                    @Header(name = HttpHeaders.CONTENT_LOCATION,
                        description = OpenApiDescriptions.CONTENT_LOCATION_DESC,
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
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
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            )
        })
    @RequestMapping(
        path = {"/api/v1/status/events/{event_id}/incident", "/api/v1/facility/events/{event_id}/incident"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncidentByEvent", version = "v1", type = MediaTypes.INCIDENT)
    public ResponseEntity<?> getIncidentByEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @PathVariable(OpenApiDescriptions.EID_NAME)
        @Parameter(description = OpenApiDescriptions.EID_NAME, required = true) String eid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[StatusController::getIncidentByEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, modifiedSince = {}, eid = {}",
                location, accept, ifModifiedSince, modifiedSince, eid);

            // Locate the specified event, then find the generatedBy relationship to identify Incident.
            Event event = repository.findEventById(eid);
            if (event != null) {
                // Look up the resource associated with the event.
                Incident incident = repository.findIncidentByHref(event.getIncidentUri());
                if (incident != null) {
                    Optional<OffsetDateTime> lastModified = Optional.ofNullable(incident.getLastModified());
                    lastModified.ifPresent(l -> 
                        headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                    // Favor the query parameter if provided.
                    final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

                    // If the request contained an If-Modified-Since header we check the entire
                    // list of resources against the specified date.  If one is newer, we return
                    // them all.
                    if (Common.notModified(ifms, lastModified.orElse(null))) {
                        // The resource has not been modified since the specified time.
                        log.debug("[StatusController::getIncidentByEvent] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }

                    return new ResponseEntity<>(incident, headers, HttpStatus.OK);
                }
            }

            log.error("[StatusController::getIncidentByEvent] incident not found {}", location);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[StatusController::getIncidentByEvent] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
