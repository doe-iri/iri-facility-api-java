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
package net.es.iri.api.facility;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
import net.es.iri.api.facility.datastore.FacilityStatusRepository;
import net.es.iri.api.facility.openapi.OpenApiDescriptions;
import net.es.iri.api.facility.schema.Discovery;
import net.es.iri.api.facility.schema.Error;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.GeographicalLocation;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.IncidentType;
import net.es.iri.api.facility.schema.Link;
import net.es.iri.api.facility.schema.Relationships;
import net.es.iri.api.facility.schema.ResolutionType;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.Site;
import net.es.iri.api.facility.schema.StatusType;
import net.es.iri.api.facility.utils.Common;
import net.es.iri.api.facility.utils.JsonParser;
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
 * The FacilityController provides API access to the IRI Facility Status functionality.
 * This class is annotated with OpenAPI documentation to autogenerate the OpenAPI v3
 * specification.
 *
 * @author hacksaw
 */
@Slf4j
@RestController
@RequestMapping(path = "/api/v1/status")
@Tag(name = "IRI Facility API", description = "Integrated Research Infrastructure Facility Status API endpoint")
public class FacilityController {
    // Spring application context.
    private final ApplicationContext context;

    // Server YAML configuration.
    private final IriConfig config;

    // The data store containing facility status information.
    private final FacilityStatusRepository repository;

    // Transformer to manipulate URL path in case there are mapping issues.
    private final UrlTransform utilities;

    /**
     * Constructor for bean injection.
     *
     * @param context The Spring application context.
     * @param config The application specific configuration.
     * @param repository The JPA-like repository holding the mock data model.
     */
    public FacilityController(ApplicationContext context, IriConfig config, FacilityStatusRepository repository) {
        this.context = context;
        this.config = config;
        this.repository = repository;
        this.utilities = new UrlTransform(Optional.ofNullable(config.getServer())
            .map(ServerConfig::getProxy).orElse(null));
    }

    /**
     * Initialize the controller.
     */
    @PostConstruct
    public void init() throws Exception {
        log.info("[FacilityController::init] initializing controller for {} - {}.",
            context.getDisplayName(), context.getApplicationName());

        // Dump the models we are loading for demo data.
        log.info("[FacilityController::init] Facilities\n{}", JsonParser.toJson(repository.findAllFacilities()));
        log.info("[FacilityController::init] Sites\n{}",      JsonParser.toJson(repository.findAllSites()));
        log.info("[FacilityController::init] Locations\n{}",  JsonParser.toJson(repository.findAllLocations()));
        log.info("[FacilityController::init] Resources\n{}",  JsonParser.toJson(repository.findAllResources()));
        log.info("[FacilityController::init] Incidents\n{}",  JsonParser.toJson(repository.findAllIncidents()));
        log.info("[FacilityController::init] Events\n{}",     JsonParser.toJson(repository.findAllEvents()));
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
    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getMetaData", version = "v1")
    public ResponseEntity<?> getMetaData() {
        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location;
            try {
                location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
            } catch (Exception ex) {
                log.error("[FacilityController::getResources] Exception caught in GET of /", ex);
                Error error = Error.builder()
                    .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                    .description(ex.getMessage())
                    .build();
                log.error("[FacilityController::getResources] returning error:\n{}", error);
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.debug("[FacilityController::getResources] GET operation = {}", location);

            // We will populate some HTTP response headers.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            List<Discovery> discovery = new ArrayList<>();
            Method[] methods = FacilityController.class.getMethods();
            for (Method m : methods) {
                if (m.isAnnotationPresent(ResourceAnnotation.class)) {
                    ResourceAnnotation ra = m.getAnnotation(ResourceAnnotation.class);
                    RequestMapping rm = m.getAnnotation(RequestMapping.class);
                    if (ra == null || rm == null) {
                        continue;
                    }

                    // Construct the URL to this resource.
                    for (String p : rm.path()) {
                        // Construct the discovery entry.
                        Discovery resource = new Discovery();
                        resource.setId(ra.name());
                        resource.setVersion(ra.version());
                        UriComponentsBuilder path = utilities.getPath(location.toASCIIString());
                        path.path(p);
                        Link link = new Link();
                        link.setRel(Relationships.SELF);
                        link.setHref(path.build().encode().toUriString());
                        resource.setLink(link);
                        discovery.add(resource);
                    }
                }
            }

            return new ResponseEntity<>(discovery, headers, HttpStatus.OK);
        } catch (SecurityException | MalformedURLException ex) {
            log.error("[FacilityController::getResources] Exception caught", ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getResources] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the facility resource associated with this endpoint.
     * <p>
     * Operation: GET /api/v1/status/facility
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
        summary = "Get the facility resource associated with this endpoint.",
        description = "Returns the facility resource associated with this endpoint.",
        tags = {"getFacility"},
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
                    schema = @Schema(implementation = Facility.class)
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
    @RequestMapping(path = "/facility", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getFacility", version = "v1")
    public ResponseEntity<?> getFacility(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location;
        try {
            location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
        } catch (Exception ex) {
            log.error("[FacilityController::getIriResources] Exception caught in GET of /resources", ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getIriResources] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.debug("[FacilityController::getFacility] GET operation = {}, accept = {}, If-Modified-Since = {}",
            location, accept, ifModifiedSince);

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        // Parse the If-Modified-Since header if it is present.
        OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

        // Find the one facility we should have populated.
        Facility facility = repository.findOneFacility();

        // The time our facility resource was last modified.
        OffsetDateTime resourceLastModified = facility.getLastModified();

        // Update the LastModified header with the value of the facility resource.
        if (resourceLastModified != null) {
            headers.setLastModified(resourceLastModified.toInstant());
        }

        log.debug("[FacilityController::getFacility] ifms {}, resourceLastModified {}", ifms, resourceLastModified);

        try {
            // If the request contained an If-Modified-Since header we process it.
            if (ifms != null && resourceLastModified != null) {
                if (ifms.isEqual(resourceLastModified) || ifms.isAfter(resourceLastModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[FacilityController::getFacility] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }
            }

            // We have success so return the models we have found.
            return new ResponseEntity<>(facility, headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            log.error("[FacilityController] getFacility failed", ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController] getFacility returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Returns the sites associated with this facility.
     * <p>
     * Operation: GET /api/v1/status/sites
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
        summary = "Get a list of sites associated with a facility.",
        description = "Returns a list of IRI resources matching the query.",
        tags = {"getSites"},
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
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = Site.class)),
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
    @RequestMapping(path = {"/sites", "/facility/sites"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getSites", version = "v1")
    public ResponseEntity<?> getSites(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getSites] GET operation = {}, accept = {}, "
                    + "If-Modified-Since = {}", location, accept, ifModifiedSince);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Site> results = repository.findAllSites();

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Site::getLastModified)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the header
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[FacilityController::getSites] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[FacilityController::getSites] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }
            }

            // Apply the shortName filter if requested.
            if (shortName != null && !shortName.isBlank()) {
                // Filter resources with the specified shortName.
                results = results.stream()
                    .filter(r -> shortName.equalsIgnoreCase(r.getShortName()))
                    .collect(Collectors.toList());
            }

            // We have success so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[FacilityController::getSites] Exception caught in GET of /sites", ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getSites] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the site associated with the specified id.
     * <p>
     * Operation: GET /api/v1/status/sites/{id}
     *            GET /api/v1/status/facility/sites/{id}
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
        summary = "Get the site associated with the specified id.",
        description = "Returns the site matching the specified id.",
        tags = {"getSite"},
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
                    schema = @Schema(implementation = Site.class)
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
        path = {"/sites/{" + OpenApiDescriptions.ID_NAME + "}",
            "/facility/sites/{" + OpenApiDescriptions.ID_NAME + "}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getSite", version = "v1")
    public ResponseEntity<?> getSite(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_MSG, required = true) String id) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getSite] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            Site site = repository.findSiteById(id);
            if (site != null) {
                OffsetDateTime lastModified = site.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since specified time.
                        log.debug("[FacilityController::getSite] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching site.
                return new ResponseEntity<>(site, headers, HttpStatus.OK);
            }

            log.error("[FacilityController::getSite] site not found {}", location);
            Error error = Error.builder()
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .description("The resource " + location + " was not found.")
                .build();
            return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getSite] Exception caught in GET of /sites/{}", id, ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getSite] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the location related to the specified site.
     * <p>
     * Operation: GET /api/v1/status/sites/{id}/location
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
        summary = "Get the location related to the specified site.",
        description = "Returns the location related to the specified site.",
        tags = {"getLocationBySite"},
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
                    schema = @Schema(implementation = GeographicalLocation.class)
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
        path = {"/sites/{" + OpenApiDescriptions.ID_NAME + "}/location",
            "/facility/sites/{" + OpenApiDescriptions.ID_NAME + "}/location"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getLocationBySite", version = "v1")
    public ResponseEntity<?> getLocationBySite(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getLocationBySite] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            Site site = repository.findSiteById(id);
            if (site!= null) {
                OffsetDateTime lastModified = site.getLastModified();
                if (lastModified != null) {
                    // Populate the header
                    headers.setLastModified(lastModified.toInstant());
                }

                // Parse the If-Modified-Since header if it is present.
                OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);
                if (ifms != null && lastModified != null) {
                    if (ifms.isEqual(lastModified) || ifms.isAfter(lastModified)) {
                        // The resource has not been modified since specified time.
                        log.debug("[FacilityController::getLocationBySite] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Look up the resource associated with the event.
                for (Link link : site.getLinks()) {
                    if (link.getRel() != null && link.getRel().equalsIgnoreCase(Relationships.LOCATED_AT)) {
                        GeographicalLocation loc = repository.findLocationByHref(link.getHref());
                        if (loc != null) {
                            return new ResponseEntity<>(loc, headers, HttpStatus.OK);
                        }
                    }
                }
            }

            log.error("[FacilityController::getLocationBySite] site not found {}", location);
            Error error = Error.builder()
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .description("The resource " + location + " was not found.")
                .build();
            return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getLocationBySite] Exception caught in GET of /sites/{}/location", id, ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getLocationBySite] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the locations associated with this facility.
     * <p>
     * Operation: GET /api/v1/status/locations
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
        summary = "Get a list of locations associated with a facility.",
        description = "Returns a list of locations matching the query.",
        tags = {"getLocations"},
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
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = GeographicalLocation.class)),
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
    @RequestMapping(path = {"/locations", "/facility/locations"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getLocations", version = "v1")
    public ResponseEntity<?> getLocations(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getLocations] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}", location, accept, ifModifiedSince);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<GeographicalLocation> results = repository.findAllLocations();

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(GeographicalLocation::getLastModified)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the header
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[FacilityController::getLocations] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[FacilityController::getLocations] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }
            }

            // We have success so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[FacilityController::getLocations] Exception caught in GET of /locations", ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getLocations] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the resources associated with this facility.
     * <p>
     * Operation: GET /api/v1/status/resources
     *            GET /api/v1/status/facility/resources
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
    @RequestMapping(path = {"/resources", "/facility/resources"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResources", version = "v1")
    public ResponseEntity<?> getResources(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.GROUP_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.GROUP_MSG) String group,
        @RequestParam(value = OpenApiDescriptions.RESOURCE_TYPE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.RESOURCE_TYPE_MSG) String type,
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getIriResources] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, group = {}, type = {}",
                location, accept, ifModifiedSince, group, type);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Resource> results = repository.findAllResources();

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Resource::getLastModified)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the header
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[FacilityController::getIriResources] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[FacilityController::getIriResources] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Now compute those resources that have changed since the If-Modified-Since time.
                results = results.stream()
                    .filter(r -> r.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the shortName filter if requested.
            if (shortName != null && !shortName.isBlank()) {
                // Filter resources with the specified shortName.
                results = results.stream()
                    .filter(r -> shortName.equalsIgnoreCase(r.getShortName()))
                    .collect(Collectors.toList());
            }

            // Apply the group filter is requested.
            if (group != null && !group.isBlank()) {
                // Filter resources from the specified group.
                results = results.stream()
                    .filter(r -> group.equalsIgnoreCase(r.getGroup()))
                    .collect(Collectors.toList());
            }

            // Apply the type filter is requested.
            if (type != null && !type.isBlank()) {
                // Filter resources from the specified type.
                results = results.stream()
                    .filter(r -> type.equalsIgnoreCase(r.getType().getValue()))
                    .collect(Collectors.toList());
            }

            // We have success so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[FacilityController::getIriResources] Exception caught in GET of /resources", ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getIriResources] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the resource associated with the specified id.
     * <p>
     * Operation: GET /api/v1/status/resources/{id}
     *            GET /api/v1/status/facility/resources/{id}
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
        path = {"/resources/{" + OpenApiDescriptions.ID_NAME + "}",
            "/facility/resources/{" + OpenApiDescriptions.ID_NAME + "}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResource", version = "v1")
    public ResponseEntity<?> getResource(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getIriResources] GET operation = {}, accept = {}, "
                    + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

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
                        // The resource has not been modified since specified time.
                        log.debug("[FacilityController::getIriResource] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching resource.
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            }

            log.error("[FacilityController::getIriResource] resource not found {}", location);
            Error error = Error.builder()
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .description("The resource " + location + " was not found.")
                .build();
            return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getIriResource] Exception caught in GET of /resources/{}", id, ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getIriResource] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns a list of "incident" resources.
     * <p>
     * Operation: GET /api/v1/status/incidents
     *            GET /api/v1/status/facility/incidents
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
        path = {"/incidents", "/facility/incidents" },
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncidents", version = "v1")
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
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getIncidents] GET operation = {}, accept = {}, "
                    + "If-Modified-Since = {}, status = {}. type = {}. resolution = {}, time = {}",
                location, accept, ifModifiedSince, status, type, resolution, time);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Incident> results = repository.findAllIncidents();

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Incident::getLastModified)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the Last-Modified header.
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[FacilityController::getIncidents] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[FacilityController::getIncidents] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Now add resources that have changed since the If-Modified-Since time.
                results = results.stream()
                    .filter(r -> r.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the shortName filter if requested.
            if (shortName != null && !shortName.isBlank()) {
                // Filter resources with the specified shortName.
                results = results.stream()
                    .filter(r -> shortName.equalsIgnoreCase(r.getShortName()))
                    .collect(Collectors.toList());
            }

            // Apply the status filter is requested.
            if (status != null && !status.isBlank()) {
                // Filter resources from the specified group.
                results = results.stream()
                    .filter(r -> status.equalsIgnoreCase(r.getStatus().getValue()))
                    .collect(Collectors.toList());
            }

            // Apply the type filter requested.
            if (type != null && !type.isBlank()) {
                // Filter resources from the specified type.
                results = results.stream()
                    .filter(r -> type.equalsIgnoreCase(r.getType().getValue()))
                    .collect(Collectors.toList());
            }

            // Apply the type filter requested.
            if (resolution != null && !resolution.isBlank()) {
                // Filter resources from the specified type.
                results = results.stream()
                    .filter(r -> resolution.equalsIgnoreCase(r.getResolution().getValue()))
                    .collect(Collectors.toList());
            }

            if (time != null && !time.isBlank()) {
                OffsetDateTime timeFilter = Common.parseTime(time);

                results = results.stream()
                    .filter(incident -> {
                        OffsetDateTime startTime = Optional.ofNullable(incident.getStart()).orElse(OffsetDateTime.MIN);
                        OffsetDateTime endTime = Optional.ofNullable(incident.getEnd()).orElse(OffsetDateTime.MAX);
                        log.debug("[FacilityController::getIncidents] startTime = {}, time = {}, endTime = {}",
                            startTime, time, endTime);
                        return (startTime.isBefore(timeFilter) || startTime.isEqual(timeFilter)) &&
                            (endTime.isAfter(timeFilter) || endTime.isEqual(timeFilter));
                    })
                    .collect(Collectors.toList());
            }

            // We have success so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[FacilityController::getIncidents] Exception caught in GET of /incidents", ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getIncidents] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the incident resource associated with the specified id.
     * <p>
     * Operation: GET /api/v1/status/incident/{id}
     *            GET /api/v1/status/facility/incident/{id}
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
        path = {"/incidents/{" + OpenApiDescriptions.ID_NAME + "}",
            "/facility/incidents/{" + OpenApiDescriptions.ID_NAME + "}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncident", version = "v1")
    public ResponseEntity<?> getIncident(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getIncidents] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

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
                        // The resource has not been modified since specified time.
                        log.debug("[FacilityController::getIncidents] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching resource.
                return new ResponseEntity<>(incident, headers, HttpStatus.OK);
            }

            log.error("[FacilityController::getIncidents] incident not found {}", location);
            Error error = Error.builder()
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .description("The resource " + location + " was not found.")
                .build();
            return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getIncidents] Exception caught in GET of /incidents/{}", id, ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getIncidents] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the events associated with the specified incident.
     * <p>
     * Operation: GET /api/v1/status/incident/{id}/events
     *            GET /api/v1/status/facility/incident/{id}/events
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
        path = {"/incidents/{" + OpenApiDescriptions.ID_NAME + "}/events",
            "/facility/incidents/{" + OpenApiDescriptions.ID_NAME + "}/events"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getEventsByIncident", version = "v1")
    public ResponseEntity<?> getEventsByIncident(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id,
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getEventsByIncident] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // Find the incident matching the specified id.
            Incident incident = repository.findIncidentById(id);
            if (incident != null) {
                // Look up the resource associated with the event.
                List<Event> events = new ArrayList<>();
                for (Link link : incident.getLinks()) {
                    if (link.getRel() != null && link.getRel().equalsIgnoreCase(Relationships.HAS_EVENT)) {
                        Event event = repository.findEventByHref(link.getHref());
                        if (event != null) {
                            events.add(event);
                        }
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
                        // The resource has not been modified since specified time.
                        log.debug("[FacilityController::getEventsByIncident] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }

                    // Now add resources that have changed since the If-Modified-Since time.
                    events = events.stream()
                        .filter(r -> r.getLastModified().isAfter(ifms))
                        .collect(Collectors.toList());
                }

                // Apply the shortName filter if requested.
                if (shortName != null && !shortName.isBlank()) {
                    // Filter resources with the specified shortName.
                    events = events.stream()
                        .filter(r -> shortName.equalsIgnoreCase(r.getShortName()))
                        .collect(Collectors.toList());
                }

                // Return the matching resource.
                return new ResponseEntity<>(events, headers, HttpStatus.OK);
            }

            log.error("[FacilityController::getEventsByIncident] event not found {}", location);
            Error error = Error.builder()
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .description("The resource " + location + " was not found.")
                .build();
            return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getEventsByIncident] Exception caught in GET of /incidents/{}/events", id, ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getEventsByIncident] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns a list of events.
     * <p>
     * Operation: GET /api/v1/status/events
     *            GET /api/v1/status/facility/events
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
    @RequestMapping(path = {"/events",
        "/facility/events"}, method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getEvents", version = "v1")
    public ResponseEntity<?> getEvents(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.STATUS_TYPE_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.STATUS_TYPE_MSG,
            schema = @Schema(implementation = StatusType.class)) String status,
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getEvents] GET operation = {}, accept = {}, "
                    + "If-Modified-Since = {}",
                location, accept, ifModifiedSince);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // We will collect the matching resources in this list.
            List<Event> results = repository.findAllEvents();

            // Parse the If-Modified-Since header if it is present.
            OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince);

            // Find the latest modified timestamp among all resources
            OffsetDateTime latestModified = results.stream()
                .map(Event::getLastModified)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());

            // Populate the Last-Modified header.
            headers.setLastModified(latestModified.toInstant());

            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer we return
            // them all.
            if (ifms != null) {
                log.debug("[FacilityController::getEvents] ifms {}, latestModified {}",
                    ifms, latestModified);
                if (ifms.isEqual(latestModified) || ifms.isAfter(latestModified)) {
                    // The resource has not been modified since specified time.
                    log.debug("[FacilityController::getEvents] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Now add resources that have changed since the If-Modified-Since time.
                results = results.stream()
                    .filter(r -> r.getLastModified().isAfter(ifms))
                    .collect(Collectors.toList());
            }

            // Apply the shortName filter if requested.
            if (shortName != null && !shortName.isBlank()) {
                // Filter resources with the specified shortName.
                results = results.stream()
                    .filter(r -> shortName.equalsIgnoreCase(r.getShortName()))
                    .collect(Collectors.toList());
            }

            // Apply the status filter is requested.
            if (status != null && !status.isBlank()) {
                // Filter resources from the specified group.
                results = results.stream()
                    .filter(r -> status.equalsIgnoreCase(r.getStatus().getValue()))
                    .collect(Collectors.toList());
            }

            // We have success so return the models we have found.
            return new ResponseEntity<>(results, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[FacilityController::getEvents] Exception caught in GET of /events", ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getEvents] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     *
     * <p>
     * Operation: GET /api/v1/status/events/{id}
     *            GET /api/v1/status/facility/events/{id}
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
        path = {"/events/{" + OpenApiDescriptions.ID_NAME + "}",
            "/facility/events/{" + OpenApiDescriptions.ID_NAME + "}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getEvent", version = "v1")
    public ResponseEntity<?> getEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

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
                        // The resource has not been modified since specified time.
                        log.debug("[FacilityController::getEvent] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }
                }

                // Return the matching resource.
                return new ResponseEntity<>(event, headers, HttpStatus.OK);
            }

            log.error("[FacilityController::getEvent] event not found {}", location);
            Error error = Error.builder()
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .description("The event " + location + " was not found.")
                .build();
            return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getEvent] Exception caught in GET of /events/{}", id, ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getEvent] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the resource impacted by the specified event.
     * <p>
     * Operation: GET /api/v1/status/events/{id}/resource
     *            GET /api/v1/status/facility/events/{id}/resource
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
        path = {"/events/{" + OpenApiDescriptions.ID_NAME + "}/resource",
            "/facility/events/{" + OpenApiDescriptions.ID_NAME + "}/resource"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getResourceByEvent", version = "v1")
    public ResponseEntity<?> getResourceByEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getResourceByEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // Find specified event then the impacted resources.
            Event event = repository.findEventById(id);
            if (event != null) {
                // Look up the resource associated with the event.
                for (Link link : event.getLinks()) {
                    if (link.getRel() != null && link.getRel().equalsIgnoreCase("impacts")) {
                        Resource resource = repository.findResourceByHref(link.getHref());
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
                                    // The resource has not been modified since specified time.
                                    log.debug("[FacilityController::getResourceByEvent] returning NOT_MODIFIED");
                                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                                }
                            }

                            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                        }
                    }
                }
            }

            log.error("[FacilityController::getResourceByEvent] event not found {}", location);
            Error error = Error.builder()
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .description("The event " + location + " was not found.")
                .build();
            return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getResourceByEvent] Exception caught in GET of /events/{}/resource", id, ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getResourceByEvent] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the incident related to the specified event.
     * <p>
     * Operation: GET /api/v1/status/events/{id}/incident
     *            GET /api/v1/status/facility/events/{id}/incident
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
        path = {"/events/{" + OpenApiDescriptions.ID_NAME + "}/incident",
            "/facility/events/{" + OpenApiDescriptions.ID_NAME + "}/incident"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getIncidentByEvent", version = "v1")
    public ResponseEntity<?> getIncidentByEvent(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @PathVariable(OpenApiDescriptions.ID_NAME)
        @Parameter(description = OpenApiDescriptions.ID_NAME, required = true) String id) {

        try {
            // We need the request URL to build fully qualified resource URLs.
            final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

            log.debug("[FacilityController::getIncidentByEvent] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", location, accept, ifModifiedSince, id);

            // Populate the content location header with our URL location.
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

            // Locate the specified event then find the memberOf relationship to identify Incident.
            Event event = repository.findEventById(id);
            if (event != null) {
                // Look up the resource associated with the event.
                for (Link link : event.getLinks()) {
                    if (link.getRel() != null && link.getRel().equalsIgnoreCase("memberOf")) {
                        Incident incident = repository.findIncidentByHref(link.getHref());
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
                                    // The resource has not been modified since specified time.
                                    log.debug("[FacilityController::getIncidentByEvent] returning NOT_MODIFIED");
                                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                                }
                            }

                            return new ResponseEntity<>(incident, headers, HttpStatus.OK);
                        }
                    }
                }
            }

            log.error("[FacilityController::getIncidentByEvent] incident not found {}", location);
            Error error = Error.builder()
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .description("The incident " + location + " was not found.")
                .build();
            return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getIncidentByEvent] Exception caught in GET of /events/{}/incident", id, ex);
            Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .description(ex.getMessage())
                .build();
            log.error("[FacilityController::getIncidentByEvent] returning error:\n{}", error);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
