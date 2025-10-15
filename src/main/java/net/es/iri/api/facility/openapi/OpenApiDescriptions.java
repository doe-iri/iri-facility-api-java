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
package net.es.iri.api.facility.openapi;

import java.net.HttpURLConnection;
import java.net.URI;
import jakarta.ws.rs.GET;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Constants for documenting the REST interface using OpenAPI annotations.
 *
 * @author hacksaw
 */
public class OpenApiDescriptions {

  /*******************************************************************************************************
   * Request header parameters.
   *******************************************************************************************************/
  public static final String ACCEPT_NAME = HttpHeaders.ACCEPT;
  public static final String ACCEPT_MSG =
      "Provides media types that are acceptable for the response. At the moment "
          + MediaType.APPLICATION_JSON_VALUE + " is the supported response encoding.";

  public static final String IF_MODIFIED_SINCE_NAME = HttpHeaders.IF_MODIFIED_SINCE;
  public static final String IF_MODIFIED_SINCE_MSG = "The HTTP request may contain the If-Modified-Since header " +
      "requesting all models with a last modified time after the specified date. The date must be specified " +
      "in RFC 1123 format.";
  public static final String IF_MODIFIED_SINCE_DEFAULT = "Thu, 02 Jan 1970 00:00:00 GMT";

  public static final String ETAG_NAME = "ETag";
  public static final String ETAG_MSG =
      "The HTTP request may contain the If-Modified-Since header requesting all models with "
          + "creationTime after the specified date. The date must be specified in RFC 1123 format.";

  public static final String IF_NONE_MATCH_NAME = "If-None-Match";
  public static final String IF_NONE_MATCH_MSG =
      "The HTTP request may contain the If-None-Match header specifying a previously provided "
          + "resource ETag value.  If the resource version identified by the provided ETag value "
          + "has not changed then a 304 NOT_MODIFIED is returned, otherwise a new version of the "
          + "resource is returned.";

  /*******************************************************************************************************
   * Response header parameters.
   *******************************************************************************************************/
  public static final String CONTENT_TYPE_NAME = HttpHeaders.CONTENT_TYPE;
  public static final String CONTENT_TYPE_DESC =
      "Provides media type used to encode the result of the operation based on those values "
          + "provided in the Accept request header. At the moment application/json is the only "
          + "supported Content-Type encoding.";

  public static final String LAST_MODIFIED_NAME = HttpHeaders.LAST_MODIFIED;
  public static final String LAST_MODIFIED_DESC =
      "The HTTP response should contain the Last-Modified header with the date set to the "
          + "RFC 1123 format of the resource's last modified time.";

  public static final String LOCATION_TYPE_NAME = HttpHeaders.LOCATION;
  public static final String LOCATION_DESC =
      "The HTTP Location header is an entity-header that gives another location for "
          + " the data that is returned and also tells how to access the resource by indicating "
          + "the direct URL.";

  public static final String CONTENT_LOCATION_NAME = HttpHeaders.CONTENT_LOCATION;
  public static final String CONTENT_LOCATION_DESC =
      "Identifies the specific resource for the representation you just returned. It provides a "
      + "URI that, at the time the response was generated, would return the same representation "
      + "if dereferenced with GET.";

  /*******************************************************************************************************
   * Query parameters.
   *******************************************************************************************************/
  public static final String MODIFIED_SINCE_NAME = "modified_since";
  public static final String MODIFIED_SINCE_MSG = "A query parameter imitating the If-Modified-Since header " +
      "requesting all models with a last modified time after the specified date. The date must be specified " +
      "in ISO 8601 format.";

  public static final String NAME_NAME = "name";
  public static final String NAME_MSG = "The name of the resource.";

  public static final String SHORT_NAME_NAME = "short_name";
  public static final String SHORT_NAME_MSG = "The short name of the resource.";

  public static final String COUNTRY_NAME_NAME = "country_name";
  public static final String COUNTRY_NAME_MSG = "The country name in which a resource resides.";

  public static final String GROUP_NAME = "group";
  public static final String GROUP_MSG =
      "The group parameter will filter resources based on group membership.  If group is specified "
          + "then only resources that are a member of the specified group will be returned.";

  public static final String STATUS_TYPE_NAME = "status";
  public static final String STATUS_TYPE_MSG = "The status of a resource.";

  public static final String INCIDENT_TYPE_NAME = "type";
  public static final String INCIDENT_TYPE_MSG = "The type of incident.";

  public static final String RESOLUTION_TYPE_NAME = "resolution";
  public static final String RESOLUTION_TYPE_MSG = "The type of resolution to the incident.";

  public static final String TIME_NAME = "time";
  public static final String TIME_MSG = "Search for incidents overlapping with time, where start <= time <= end.  The time query parameter must be in ISO 8601 format with timezone offsets.";

  public static final String FROM_NAME = "from";
  public static final String FROM_MSG = "Search for incidents/events that are active after (and including) the specified from time.  " +
      "The from query parameter must be in ISO 8601 format with timezone offsets.";

  public static final String TO_NAME = "to";
  public static final String TO_MSG = "Search for incidents/events that are active before (and including) the specified 'to' time.  " +
      "The from query parameter must be in ISO 8601 format with timezone offsets.";

  public static final String RESOURCE_TYPE_NAME = "resource_type";
  public static final String RESOURCE_TYPE_MSG = "Return only resources of this type.";

  public static final String RESOURCE_CAPABILITY_NAME = "capability";
  public static final String RESOURCE_CAPABILITY_MSG = "Return only resources with this capability.";

  public static final String CURRENT_STATUS_NAME = "current_status";
  public static final String CURRENT_STATUS_MSG = "Return only resources with these status values.";

  public static final String RESOURCES_NAME = "resources";
  public static final String RESOURCES_MSG = "A list of resources to match.";

  public static final String USERIDS_NAME = "user_ids";
  public static final String USERIDS_MSG = "Return only projects containing any of the listed users.";

  public static final String OFFSET_NAME = "offset";
  public static final String OFFSET_MSG = "An integer value specifying the starting number of resource to " +
      "return, where 0 is the first resource based on identifier sorted order.";

  public static final String LIMIT_NAME = "limit";
  public static final String LIMIT_MSG = "An integer value specifying the maximum number of resources to return, " +
      "where 100 is the default limit.  Use offset query parameter to navigate retrieval of complete set.";

  /*******************************************************************************************************
   * URL path parameters.
   *******************************************************************************************************/
  public static final String ID_NAME = "id";
  public static final String ID_MSG = "The UUID uniquely identifying the IRI resource.";

  public static final String SID_NAME = "site_id";
  public static final String SID_MSG = "The UUID uniquely identifying the IRI site resource.";

  public static final String LID_NAME = "location_id";
  public static final String LID_MSG = "The UUID uniquely identifying the IRI location resource.";

  public static final String RID_NAME = "resource_id";
  public static final String RID_MSG = "The UUID uniquely identifying the IRI resource resource.";

  public static final String IID_NAME = "incident_id";
  public static final String IID_MSG = "The UUID uniquely identifying the IRI incident resource.";

  public static final String EID_NAME = "event_id";
  public static final String EID_MSG = "The UUID uniquely identifying the IRI event resource.";

  public static final String CID_NAME = "capability_id";
  public static final String CID_MSG = "The UUID uniquely identifying the IRI capability resource.";

  public static final String PID_NAME = "project_id";
  public static final String PID_MSG = "The UUID uniquely identifying the IRI project resource.";

  public static final String PAID_NAME = "project_allocation_id";
  public static final String PAID_MSG = "The UUID uniquely identifying the IRI project allocation resource.";

  public static final String UAID_NAME = "user_allocation_id";
  public static final String UAID_MSG = "The UUID uniquely identifying the IRI user allocation resource.";

  /*******************************************************************************************************
   * HTTP Response Codes and messages.
   *******************************************************************************************************/
  public static final String OK_CODE = "" + HttpURLConnection.HTTP_OK;

  public static final String OK_MSG = "OK - Returns a list of available IRI resources "
      + "matching the request criteria.";

  public static final String CREATED_CODE = "" + HttpURLConnection.HTTP_CREATED;
  public static final String CREATED_MSG =
      "Created - Indicates the Resource Manager is willing to complete the requested model "
          + "delta, and has created a delta resource to track the agreed change. A JSON "
          + "structure containing the newly created delta resource will be returned tracking the "
          + "agreed to changes, and the resulting topology model.";

  public static final String NO_CONTENT_CODE = "" + HttpURLConnection.HTTP_NO_CONTENT;
  public static final String NO_CONTENT_MSG =
      "Committed - Indicates the Resource Manager has committed the requested model delta.";

  public static final String NOT_MODIFIED_CODE = "" + HttpURLConnection.HTTP_NOT_MODIFIED;
  public static final String NOT_MODIFIED_MSG =
      "Not Modified - The requested resource was not modified.";

  public static final String BAD_REQUEST_CODE = "" + HttpURLConnection.HTTP_BAD_REQUEST;
  public static final String BAD_REQUEST_MSG =
      "Bad Request - The server due to malformed syntax or invalid query parameters could "
          + "not understand the client’s request.";

  public static final String UNAUTHORIZED_CODE = "" + HttpURLConnection.HTTP_UNAUTHORIZED;
  public static final String UNAUTHORIZED_MSG =
      "Unauthorized - Requester is not authorized to access the requested resource.";

  public static final String FORBIDDEN_CODE = "" + HttpURLConnection.HTTP_FORBIDDEN;
  public static final String FORBIDDEN_MSG =
      "Forbidden - Requester is not authorized to access the requested resource.";

  public static final String NOT_FOUND_CODE = "" + HttpURLConnection.HTTP_NOT_FOUND;
  public static final String NOT_FOUND_MSG =
      "Not Found - The provider is not currently capable of serving resource models.";

  public static final String NOT_ACCEPTABLE_CODE = "" + HttpURLConnection.HTTP_NOT_ACCEPTABLE;
  public static final String NOT_ACCEPTABLE_MSG =
      "Not Acceptable - The requested resource is capable of generating only content not "
          + "acceptable according to the Accept headers sent in the request.";

  public static final String CONFLICT_CODE = "" + HttpURLConnection.HTTP_CONFLICT;
  public static final String CONFLICT_MSG =
      "Conflict - The request could not be completed due to a conflict with the current "
          + " state of the resource (model).";

  public static final String INTERNAL_ERROR_CODE = "" + HttpURLConnection.HTTP_INTERNAL_ERROR;
  public static final String INTERNAL_ERROR_MSG =
      "Internal Server Error - A generic error message given when an unexpected condition "
          + "was encountered and a more specific message is not available.";

}
