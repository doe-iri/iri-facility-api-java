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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
import net.es.iri.api.facility.schema.Discovery;
import net.es.iri.api.facility.schema.Error;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Location;
import net.es.iri.api.facility.schema.MediaTypes;
import net.es.iri.api.facility.schema.NamedObject;
import net.es.iri.api.facility.schema.Site;
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
 * The FacilityController provides API access to the IRI Facility functionality.
 * This class is annotated with OpenAPI documentation to autogenerate the OpenAPI v3
 * specification.
 *
 * @author hacksaw
 */
@Slf4j
@RestController
@Tag(name = "IRI Facility API", description = "The Integrated Research Infrastructure Facility API endpoint.")
public class FacilityController {
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
     */
    public FacilityController(ApplicationContext context, IriConfig config, FacilityDataRepository repository) {
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
        log.info("[FacilityController::init] initializing controller for {} - {}.",
            context.getDisplayName(), context.getApplicationName());
    }

    /**
     * Returns a list of available Facility API resource endpoints.
     * <p>
     * Operation: GET /api/v1
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get a list of facility API meta-data.",
        description = "Returns a list of available facility URL.",
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
    @RequestMapping(
        path = {"/api/v1"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getMetaData", version = "v1", type = MediaTypes.DISCOVERY)
    public ResponseEntity<?> getMetaData(Locale locale) {
        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // We will populate some HTTP response headers.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[FacilityController::getMetaData] GET operation = {}", location);

            // Get a list of all available methods to search for annotations.
            List<Method> methods = Stream.of(
                    FacilityController.class.getMethods(),
                    StatusController.class.getMethods(),
                    AccountController.class.getMethods()
                )
                .flatMap(Arrays::stream)
                .toList();

            // Create the list of metadata entries.
            List<Discovery> discovery = Discovery.getDiscovery(utilities, location.toASCIIString(),
                methods, "/api/*");

            return new ResponseEntity<>(discovery, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("[FacilityController::getMetaData] Exception caught for {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the facility resource associated with this endpoint.
     * <p>
     * Operation: GET /api/v1/facility
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
        path = "/api/v1/facility",
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getFacility", version = "v1", type = MediaTypes.FACILITY)
    public ResponseEntity<?> getFacility(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince) {

        log.debug("[FacilityController::getFacility] GET accept = {}, If-Modified-Since = {}, modifiedSince = {}",
            accept, ifModifiedSince, modifiedSince);

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        // Parse the If-Modified-Since header if it is present.
        final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

        // Find the one facility we should have populated.
        Facility facility = repository.findOneFacility();

        // The time our facility resource was last modified.
        Optional<OffsetDateTime> lastModified = Optional.ofNullable(facility.getLastModified());
        lastModified.ifPresent(l ->
            headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

        try {
            // If the request contained an If-Modified-Since header we check the entire
            // list of resources against the specified date.  If one is newer, we return
            // them all.
            if (Common.notModified(ifms, lastModified.orElse(null))) {
                // The resource has not been modified since the specified time.
                log.debug("[FacilityController::getFacility] returning NOT_MODIFIED");
                return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
            }

            // We have success, so return the models we have found.
            return new ResponseEntity<>(facility, headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            log.error("[FacilityController::getFacility] getFacility failed for {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the sites associated with this facility.
     * <p>
     * Operation: GET /api/v1/facility/sites
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
        path = {"/api/v1/facility/sites"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getSites", version = "v1", type = MediaTypes.SITES)
    public ResponseEntity<?> getSites(
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
        @Parameter(description = OpenApiDescriptions.OFFSET_MSG, schema = @Schema(type = "integer", defaultValue = "0")) Integer offset,
        @RequestParam(value = OpenApiDescriptions.LIMIT_NAME, required = false, defaultValue = "100")
        @Parameter(description = OpenApiDescriptions.LIMIT_MSG, schema = @Schema(type = "integer", defaultValue = "100")) Integer limit,
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[FacilityController::getSites] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, modifiedSince = {}, name = {}, offset = {}, limit = {}, shortName = {}",
                location, accept, ifModifiedSince, modifiedSince, name, offset, limit, shortName);

            // Favor the query parameter if provided.
            final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

            // We will collect the matching resources in this list.
            List<Site> results = repository.findAllSites();

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

            // Apply the shortName filter if requested.
            if (shortName != null && !shortName.isBlank()) {
                // Filter resources with the specified shortName.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(shortName).equalsIgnoreCase(r.getShortName()))
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
            log.error("[FacilityController::getSites] Exception caught in GET of /sites", ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the site associated with the specified id.
     * <p>
     * Operation: GET /api/v1/facility/sites/{site_id}
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment, 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param sid The site_id of the target resource.
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
        path = {"/api/v1/facility/sites/{site_id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getSite", version = "v1", type = MediaTypes.SITE)
    public ResponseEntity<?> getSite(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @PathVariable(OpenApiDescriptions.SID_NAME)
        @Parameter(description = OpenApiDescriptions.SID_MSG, required = true) String sid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[FacilityController::getSite] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, modifiedSince = {}, sid = {}",
                location, accept, ifModifiedSince, modifiedSince, sid);


            // We will collect the matching resources in this list.
            Site site = repository.findSiteById(sid);
            if (site != null) {
                Optional<OffsetDateTime> lastModified = Optional.ofNullable(site.getLastModified());
                lastModified.ifPresent(l ->
                    headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                // Favor the query parameter if provided.
                final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

                // If the request contained an If-Modified-Since header we check the entire
                // list of resources against the specified date.  If one is newer, we return
                // them all.
                if (Common.notModified(ifms, lastModified.orElse(null))) {
                    // The resource has not been modified since the specified time.
                    log.debug("[FacilityController::getSite] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Return the matching site.
                return new ResponseEntity<>(site, headers, HttpStatus.OK);
            }

            log.error("[FacilityController::getSite] site not found {}", location);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getSite] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the location related to the specified site.
     * <p>
     * Operation: GET /api/v1/status/sites/{site_id}/location
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param sid The site_id of the target location resource.
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
                    schema = @Schema(implementation = Location.class)
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
        path = {"/api/v1/facility/sites/{site_id}/location"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getLocationBySite", version = "v1", type = MediaTypes.LOCATION)
    public ResponseEntity<?> getLocationBySite(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @PathVariable(OpenApiDescriptions.SID_NAME)
        @Parameter(description = OpenApiDescriptions.SID_NAME, required = true) String sid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
            log.debug("[FacilityController::getLocationBySite] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, modifiedSince = {}, sid = {}",
                location, accept, ifModifiedSince, modifiedSince, sid);

            // We will collect the matching resources in this list.
            Site site = repository.findSiteById(sid);
            if (site!= null && site.getLocationUri() != null) {
                // Look up the location associated with the site.
                Location loc = repository.findLocationByHref(site.getLocationUri());
                if (loc != null) {
                    Optional<OffsetDateTime> lastModified = Optional.ofNullable(loc.getLastModified());
                    lastModified.ifPresent(l ->
                        headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                    // Favor the query parameter if provided.
                    final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

                    // If the request contained an If-Modified-Since header we check the entire
                    // list of resources against the specified date.  If one is newer, we return
                    // them all.
                    if (Common.notModified(ifms, lastModified.orElse(null))) {
                        // The resource has not been modified since the specified time.
                        log.debug("[FacilityController::getLocationBySit] returning NOT_MODIFIED");
                        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                    }

                    return new ResponseEntity<>(loc, headers, HttpStatus.OK);
                }
            }

            log.error("[FacilityController::getLocationBySite] site not found {}", location);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(location), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getLocationBySite] Exception caught in GET of {}", location, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the locations associated with this facility.
     * <p>
     * Operation: GET /api/v1/facility/locations
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment, 'application/json' is the supported response encoding.
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
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = Location.class)),
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
    @RequestMapping(path = {"/api/v1/facility/locations"},
        method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getLocations", version = "v1", type = MediaTypes.LOCATIONS)
    public ResponseEntity<?> getLocations(
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
        @RequestParam(value = OpenApiDescriptions.SHORT_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.SHORT_NAME_MSG) String shortName,
        @RequestParam(value = OpenApiDescriptions.COUNTRY_NAME_NAME, required = false)
        @Parameter(description = OpenApiDescriptions.COUNTRY_NAME_MSG) String countryName) {

        // We need the request URL to build fully qualified resource URLs.
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

        try {
             log.debug("[FacilityController::getLocations] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, modifiedSince = {}, name = {}, offset = {}, limit = {}, shortName = {}",
                 location, accept, ifModifiedSince, modifiedSince, name, offset, limit,  shortName);

            // Favor the query parameter if provided.
            final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

            // We will collect the matching resources in this list.
            List<Location> results = repository.findAllLocations();

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
                log.debug("[FacilityController::getLocations] returning NOT_MODIFIED");
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

            // Apply the shortName filter if requested.
            if (shortName != null && !shortName.isBlank()) {
                // Filter resources with the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(shortName).equalsIgnoreCase(r.getShortName()))
                    .collect(Collectors.toList());
            }

            // Apply the countryName filter if requested.
            if (countryName != null && !countryName.isBlank()) {
                // Filter resources with the specified name.
                results = results.stream()
                    .filter(r -> Common.stripQuotes(countryName).equalsIgnoreCase(r.getCountryName()))
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
            log.error("[FacilityController::getLocations] Exception caught in GET of /locations", ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(location, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the location associated with the specified id.
     * <p>
     * Operation: GET /api/v1/facility/locations/{location_id}
     *
     * @param accept Provides media types that are acceptable for the response.
     *    At the moment, 'application/json' is the supported response encoding.
     *
     * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
     *    header requesting all resources with lastModified after the specified
     *    date. The date must be specified in RFC 1123 format.
     *
     * @param lid The location_id of the target resource.
     *
     * @return A RESTful response.
     */
    @Operation(
        summary = "Get the IRI Location associated with the specified id.",
        description = "Returns the matching IRI location.",
        tags = {"getLocation"},
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
                    schema = @Schema(implementation = Location.class)
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
        path = {"/api/v1/facility/locations/{location_id}"},
        method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    @ResponseBody
    @ResourceAnnotation(name = "getLocation", version = "v1", type = MediaTypes.LOCATION)
    public ResponseEntity<?> getLocation(
        @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE)
        @Parameter(description = OpenApiDescriptions.ACCEPT_MSG) String accept,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
        @Parameter(description = OpenApiDescriptions.IF_MODIFIED_SINCE_MSG) String ifModifiedSince,
        @RequestParam(value = OpenApiDescriptions.MODIFIED_SINCE_NAME, required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = OpenApiDescriptions.MODIFIED_SINCE_MSG) OffsetDateTime modifiedSince,
        @PathVariable(OpenApiDescriptions.LID_NAME)
        @Parameter(description = OpenApiDescriptions.LID_NAME, required = true) String lid) {

        // We need the request URL to build fully qualified resource URLs.
        final URI loc = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        // Populate the content location header with our URL location.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, loc.toASCIIString());

        try {
            log.debug("[FacilityController::getLocation] GET operation = {}, accept = {}, "
                + "If-Modified-Since = {}, id = {}", loc, accept, ifModifiedSince, lid);

            // Find the resource targeted by id.
            Location location = repository.findLocationById(lid);
            if (location != null) {
                Optional<OffsetDateTime> lastModified = Optional.ofNullable(location.getLastModified());
                lastModified.ifPresent(l ->
                    headers.setLastModified(l.toInstant().truncatedTo(ChronoUnit.SECONDS)));

                // Favor the query parameter if provided.
                final OffsetDateTime ifms = Common.parseIfModifiedSince(ifModifiedSince, modifiedSince);

                // If the request contained an If-Modified-Since header we check the entire
                // list of resources against the specified date.  If one is newer, we return
                // them all.
                if (Common.notModified(ifms, lastModified.orElse(null))) {
                    // The resource has not been modified since the specified time.
                    log.debug("[FacilityController::getLocation] returning NOT_MODIFIED");
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }

                // Return the matching resource.
                return new ResponseEntity<>(location, headers, HttpStatus.OK);
            }

            log.error("[FacilityController::getLocation] location not found {}", loc);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.notFoundError(loc), headers, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            log.error("[FacilityController::getLocation] Exception caught in GET of {}", loc, ex);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            return new ResponseEntity<>(Common.internalServerError(loc, ex), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
