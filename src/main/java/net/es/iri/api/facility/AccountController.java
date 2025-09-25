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
import net.es.iri.api.facility.openapi.OpenApiDescriptions;
import net.es.iri.api.facility.schema.Capability;
import net.es.iri.api.facility.schema.Discovery;
import net.es.iri.api.facility.schema.Error;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.IncidentType;
import net.es.iri.api.facility.schema.Link;
import net.es.iri.api.facility.schema.Location;
import net.es.iri.api.facility.schema.MediaTypes;
import net.es.iri.api.facility.schema.Project;
import net.es.iri.api.facility.schema.ProjectAllocation;
import net.es.iri.api.facility.schema.Relationships;
import net.es.iri.api.facility.schema.ResolutionType;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.Site;
import net.es.iri.api.facility.schema.StatusType;
import net.es.iri.api.facility.schema.UserAllocation;
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
 * The AccountController provides API access to the IRI Facility Account functionality.
 * This class is annotated with OpenAPI documentation to autogenerate the OpenAPI v3
 * specification.
 *
 * @author hacksaw
 */
@Slf4j
@RestController
@Tag(name = "IRI Facility Account API", description = "Integrated Research Infrastructure Facility Account API endpoint.")
public class AccountController {
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
    public AccountController(ApplicationContext context, IriConfig config, FacilityDataRepository repository) {
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
        log.info("[AccountController::init] initializing controller for {} - {}.",
            context.getDisplayName(), context.getApplicationName());
    }

    /**
     * Returns a list of available Facility Account API resource endpoints.
     * <p>
     * Operation: GET /api/v1/account
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
    @RequestMapping(path = {"/api/v1/account"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getMetaData", version = "v1", type = MediaTypes.DISCOVERY)
    public ResponseEntity<?> getMetaData() {
        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getMetaData] GET operation = {}", location);

            // We will populate some HTTP response headers.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            List<Method> methods = Stream.of(AccountController.class.getMethods()).toList();
            List<Discovery> discovery = FacilityController.getDiscovery(utilities, location.toASCIIString(),  methods);

            return new ResponseEntity<>(discovery, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[AccountController::getMetaData] Exception caught", ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the capabilities associated with this facility.
     * <p>
     * Operation: GET /api/v1/account/capabilities
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
        summary = "Get a list of capabilities associated with a facility.",
        description = "Returns a list of IRI resources matching the query.",
        tags = {"getCapabilities"},
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
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = Capability.class)),
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
    @RequestMapping(path = {"/api/v1/account/capabilities"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getCapabilities", version = "v1", type = MediaTypes.CAPABILITIES)
    public ResponseEntity<?> getCapabilities(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.NAME_MSG) String name) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getCapabilities] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}", location, accept, ifModifiedSince);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Capability> results = repository.findAllCapabilities();

            results.forEach(c ->
                log.debug("[AccountController::getCapabilities] id = {}, object = {}", c.getId(), c));

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Capability::getLastModified)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the header
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[AccountController::getCapabilities] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[AccountController::getCapabilities] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }
            }

            // Apply the name filter if requested.
            if (name != null && !name.isBlank()) {
                // Filter resources with the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(name).equalsIgnoreCase(r.getName()))
                    .collect(Collectors.toList());
            }

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[AccountController::getCapabilities] Exception caught in GET of /capabilities", ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the capability associated with the specified id.
     * <p>
     * Operation: GET /api/v1/account/capabilities/{id}
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
        summary = "Get the capability associated with the specified id.",
        description = "Returns the capability matching the specified id.",
        tags = {"getCapability"},
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
                    schema = @Schema(implementation = Capability.class)
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
        path = {"/api/v1/account/capabilities/{id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getCapability", version = "v1", type = MediaTypes.CAPABILITY)
    public ResponseEntity<?> getCapability(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_MSG, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getCapability] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            Capability capability = repository.findCapabilityById(id);
            if (capability != null) {
                OffsetDateTime lastModified = capability.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since the specified time.
                        log.debug("[AccountController::getCapability] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching site.
                return new ResponseEntity<>(capability, headers, HttpStatus.OK);
            }

            log.error("[AccountController::getCapability] capability not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[AccountController::getCapability] Exception caught in GET of /capabilities/{}", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the projects associated with this facility.
     * <p>
     * Operation: GET /api/v1/account/projects
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
        summary = "Get a list of projects associated with a facility.",
        description = "Returns a list of IRI projects matching the query.",
        tags = {"getProjects"},
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
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = Project.class)),
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
    @RequestMapping(path = {"/api/v1/account/projects"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getProjects", version = "v1", type = MediaTypes.PROJECTS)
    public ResponseEntity<?> getProjects(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.NAME_MSG) String name,
        @RequestParam(value = OpenApiDescriptions.USERIDS_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.USERIDS_MSG) List<String> userIds) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getProjects] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}", location, accept, ifModifiedSince);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Project> results = repository.findAllProjects();

            results.forEach(c ->
                log.debug("[AccountController::getProjects] id = {}, object = {}", c.getId(), c));

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Project::getLastModified)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the header
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[AccountController::getProjects] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[AccountController::getProjects] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }
            }

            // Apply the name filter if requested.
            if (name != null && !name.isBlank()) {
                // Filter resources with the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(name).equalsIgnoreCase(r.getName()))
                    .collect(Collectors.toList());
            }

            // Apply the userId filter if requested.
            if (userIds != null && !userIds.isEmpty()) {
                  // Normalize inputs once
                final List<String> normalized = userIds.stream()
                    .filter(Objects::nonNull)
                    .map(Common::stripQuotes)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
                if (!normalized.isEmpty()) {
                    results = results.stream()
                        .filter(r -> {
                            List<String> uris = r.getUserIds();
                            return normalized.stream().anyMatch(cap -> Common.contains(cap, uris));
                        })
                        .collect(Collectors.toList());
                }
            }

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[AccountController::getProjects] Exception caught in GET of /projects", ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the project associated with the specified id.
     * <p>
     * Operation: GET /api/v1/account/project/{id}
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
        summary = "Get the project associated with the specified id.",
        description = "Returns the project matching the specified id.",
        tags = {"getProject"},
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
                    schema = @Schema(implementation = Project.class)
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
        path = {"/api/v1/account/projects/{id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getProject", version = "v1", type = MediaTypes.PROJECT)
    public ResponseEntity<?> getProject(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_MSG, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getProject] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            Project result = repository.findProjectById(id);
            if (result != null) {
                OffsetDateTime lastModified = result.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since the specified time.
                        log.debug("[AccountController::getProject] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching site.
                return new ResponseEntity<>(result, headers, HttpStatus.OK);
            }

            log.error("[AccountController::getProject] project not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[AccountController::getProject] Exception caught in GET of /projects/{}", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the project allocations associated with this facility.
     * <p>
     * Operation: GET /api/v1/account/project_allocations
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
        summary = "Get a list of project allocations associated with a facility.",
        description = "Returns a list of IRI project allocations matching the query.",
        tags = {"getProjectAllocations"},
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
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectAllocation.class)),
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
    @RequestMapping(path = {"/api/v1/account/project_allocations"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getProjectAllocations", version = "v1", type = MediaTypes.PROJECT_ALLOCATIONS)
    public ResponseEntity<?> getProjectAllocations(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.NAME_MSG) String name,
        @RequestParam(value = OpenApiDescriptions.USERIDS_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.USERIDS_MSG) List<String> userIds) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getProjectAllocations] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}", location, accept, ifModifiedSince);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<ProjectAllocation> results = repository.findAllProjectAllocations();

            results.forEach(c ->
                log.debug("[AccountController::getProjectAllocations] id = {}, object = {}", c.getId(), c));

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(ProjectAllocation::getLastModified)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the header
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[AccountController::getProjectAllocations] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[AccountController::getProjectAllocations] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }
            }

            // Apply the name filter if requested.
            if (name != null && !name.isBlank()) {
                // Filter resources with the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(name).equalsIgnoreCase(r.getName()))
                    .collect(Collectors.toList());
            }

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[AccountController::getProjectAllocations] Exception caught in GET of /project_allocations", ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the project allocation associated with the specified id.
     * <p>
     * Operation: GET /api/v1/account/project_allocations/{id}
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
        summary = "Get the project allocation associated with the specified id.",
        description = "Returns the project allocation matching the specified id.",
        tags = {"getProjectAllocation"},
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
                    schema = @Schema(implementation = ProjectAllocation.class)
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
        path = {"/api/v1/account/project_allocations/{id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getProjectAllocation", version = "v1", type = MediaTypes.PROJECT_ALLOCATION)
    public ResponseEntity<?> getProjectAllocation(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_MSG, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getProjectAllocation] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            ProjectAllocation result = repository.findProjectAllocationById(id);
            if (result != null) {
                OffsetDateTime lastModified = result.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since the specified time.
                        log.debug("[AccountController::getProjectAllocation] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching site.
                return new ResponseEntity<>(result, headers, HttpStatus.OK);
            }

            log.error("[AccountController::getProjectAllocation] project not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[AccountController::getProjectAllocation] Exception caught in GET of /project_allocations/{}", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the user allocations associated with this facility.
     * <p>
     * Operation: GET /api/v1/account/user_allocations
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
        summary = "Get a list of user allocations associated with a facility.",
        description = "Returns a list of IRI user allocations matching the query.",
        tags = {"getUserAllocations"},
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
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserAllocation.class)),
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
    @RequestMapping(path = {"/api/v1/account/user_allocations"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getUserAllocations", version = "v1", type = MediaTypes.USER_ALLOCATIONS)
    public ResponseEntity<?> getUserAllocations(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.NAME_MSG) String name,
        @RequestParam(value = OpenApiDescriptions.USERIDS_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.USERIDS_MSG) List<String> userIds) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getUserAllocations] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}", location, accept, ifModifiedSince);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<UserAllocation> results = repository.findAllUserAllocations();

            results.forEach(c ->
                log.debug("[AccountController::getUserAllocations] id = {}, object = {}", c.getId(), c));

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(UserAllocation::getLastModified)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the header
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[AccountController::getUserAllocations] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[AccountController::getUserAllocations] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }
            }

            // Apply the name filter if requested.
            if (name != null && !name.isBlank()) {
                // Filter resources with the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(name).equalsIgnoreCase(r.getName()))
                    .collect(Collectors.toList());
            }

            // We have success, so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[AccountController::getUserAllocations] Exception caught in GET of /user_allocations", ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the user allocation associated with the specified id.
     * <p>
     * Operation: GET /api/v1/account/user_allocations/{id}
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
        summary = "Get the user allocation associated with the specified id.",
        description = "Returns the user allocation matching the specified id.",
        tags = {"getUserAllocation"},
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
                    schema = @Schema(implementation = UserAllocation.class)
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
        path = {"/api/v1/account/user_allocations/{id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getUserAllocation", version = "v1", type = MediaTypes.USER_ALLOCATION)
    public ResponseEntity<?> getUserAllocation(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_MSG, required = true) String id) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        try {
            log.debug("[AccountController::getUserAllocation] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            UserAllocation result = repository.findUserAllocationById(id);
            if (result != null) {
                OffsetDateTime lastModified = result.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since the specified time.
                        log.debug("[AccountController::getUserAllocation] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching site.
                return new ResponseEntity<>(result, headers, HttpStatus.OK);
            }

            log.error("[AccountController::getUserAllocation] user not found {}", location);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[AccountController::getUserAllocation] Exception caught in GET of /user_allocations/{}", id, ex);
            return new ResponseEntity<>(Common.internalServerError(location, ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
