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
package net.es.iri.api.facility.mapping;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.FacilityController;
import net.es.iri.api.facility.datastore.FacilityStatusRepository;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.EventEmbedded;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.FacilityEmbedded;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.IncidentEmbedded;
import net.es.iri.api.facility.schema.Link;
import net.es.iri.api.facility.schema.Location;
import net.es.iri.api.facility.schema.LocationEmbedded;
import net.es.iri.api.facility.schema.Relationships;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.ResourceEmbedded;
import net.es.iri.api.facility.schema.Site;
import net.es.iri.api.facility.schema.SiteEmbedded;

@Slf4j
public class EmbeddedMapping {
    /**
     * Process the request to include embedded objects in the Facility object.
     *
     * @param facility The Facility object containing the relationships.
     * @param includes The list of relationships to dereference and include in the embedded property.
     * @return Map of embedded objects to include in the results.
     */
    public static FacilityEmbedded processIncludes(Facility facility,
                                                   FacilityStatusRepository repository, List<String> includes) {
        FacilityEmbedded results = new FacilityEmbedded();

        // Ensure we have a list of includes to process.
        if (includes != null) {
            // For each relationship listed, we need to find matching links in the provided resource.
            for (String rel : includes) {
                log.debug("[EmbeddedMapping::processIncludes] rel = {}", rel);
                for (Link link : facility.getLinks()) {
                    if (link.getRel().contains(rel)) {
                        Mapping reference = Mapping.getTargetClass(FacilityMapping.RELATIONSHIP_TARGETS, link.getRel());
                        reference.getNamedObject(repository, link.getHref()).ifPresent(o -> {
                            switch (link.getRel()) {
                                case Relationships.HOSTED_AT:
                                    if (results.getHostedAt() == null) {
                                        results.setHostedAt(new ArrayList<>());
                                    }
                                    results.getHostedAt().add((Site) o);
                                    break;
                                case Relationships.HAS_LOCATION:
                                    if (results.getHasLocation() == null) {
                                        results.setHasLocation(new ArrayList<>());
                                    }
                                    results.getHasLocation().add((Location) o);
                                    break;
                                case Relationships.HAS_INCIDENT:
                                    if (results.getHasIncident() == null) {
                                        results.setHasIncident(new ArrayList<>());
                                    }
                                    results.getHasIncident().add((Incident) o);
                                    break;
                                case Relationships.HAS_EVENT:
                                    if (results.getHasEvent() == null) {
                                        results.setHasEvent(new ArrayList<>());
                                    }
                                    results.getHasEvent().add((Event) o);
                                    break;
                                case Relationships.HAS_RESOURCE:
                                    if (results.getHasResource() == null) {
                                        results.setHasResource(new ArrayList<>());
                                    }
                                    results.getHasResource().add((Resource) o);
                                    break;
                                default:
                                    log.error("[EmbeddedMapping::processIncludes] invalid relationship for facility {}, rel = {}",
                                        facility.getId(), link.getRel());
                                    break;
                            }});
                    }
                }
            }
        }

        return results.isEmpty() ? null : results;
    }

    /**
     * Process the request to include embedded objects in the Incident object.
     *
     * @param incident The Facility object containing the relationships.
     * @param includes The list of relationships to dereference and include in the embedded property.
     * @return Map of embedded objects to include in the results.
     */
    public static IncidentEmbedded processIncludes(Incident incident, FacilityStatusRepository repository,
                                             List<String> includes) {
        IncidentEmbedded results = new IncidentEmbedded();

        // Ensure we have a list of includes to process.
        if (includes != null) {
            // For each relationship listed, we need to find matching links in the provided resource.
            for (String rel : includes) {
                log.debug("[EmbeddedMapping::processIncludes] rel = {}", rel);
                for (Link link : incident.getLinks()) {
                    if (link.getRel().contains(rel)) {
                        Mapping reference = Mapping.getTargetClass(IncidentMapping.RELATIONSHIP_TARGETS, link.getRel());
                        reference.getNamedObject(repository, link.getHref()).ifPresent(o -> {
                            switch (link.getRel()) {
                                case Relationships.HAS_EVENT:
                                    if (results.getHasEvent() == null) {
                                        results.setHasEvent(new ArrayList<>());
                                    }
                                    results.getHasEvent().add((Event) o);
                                    break;
                                case Relationships.MAY_IMPACT:
                                    if (results.getMayImpact() == null) {
                                        results.setMayImpact(new ArrayList<>());
                                    }
                                    results.getMayImpact().add((Resource) o);
                                    break;
                                default:
                                    log.error("[EmbeddedMapping::processIncludes] invalid relationship for incident {}, rel = {}",
                                        incident.getId(), link.getRel());
                                    break;
                            }});
                    }
                }
            }
        }

        return results.isEmpty() ? null : results;
    }

    /**
     * Process the request to include embedded objects in the Event object.
     *
     * @param event The Event object containing the relationships.
     * @param includes The list of relationships to dereference and include in the embedded property.
     * @return Map of embedded objects to include in the results.
     */
    public static  EventEmbedded processIncludes(Event event, FacilityStatusRepository repository,
                                                 List<String> includes) {
        EventEmbedded results = new EventEmbedded();

        // Ensure we have a list of includes to process.
        if (includes != null) {
            // For each relationship listed, we need to find matching links in the provided resource.
            for (String rel : includes) {
                log.debug("[EmbeddedMapping::processIncludes] rel = {}", rel);
                for (Link link : event.getLinks()) {
                    if (link.getRel().contains(rel)) {
                        Mapping reference = Mapping.getTargetClass(EventMapping.RELATIONSHIP_TARGETS, link.getRel());
                        reference.getNamedObject(repository, link.getHref()).ifPresent(o -> {
                            switch (link.getRel()) {
                                case Relationships.GENERATED_BY:
                                    if (results.getGeneratedBy() == null) {
                                        results.setGeneratedBy(new ArrayList<>());
                                    }
                                    results.getGeneratedBy().add((Incident) o);
                                    break;
                                case Relationships.IMPACTS:
                                    if (results.getImpacts() == null) {
                                        results.setImpacts(new ArrayList<>());
                                    }
                                    results.getImpacts().add((Resource) o);
                                    break;
                                default:
                                    log.error("[EmbeddedMapping::processIncludes] invalid relationship for event {}, rel = {}",
                                        event.getId(), link.getRel());
                                    break;
                            }});
                    }
                }
            }
        }

        return results.isEmpty() ? null : results;
    }

    /**
     * Handles the request to include embedded objects within the Resource object.
     *
     * @param resource: The Resource object that contains the relationships.
     * @param includes: The list of relationships to dereference and add to the embedded property.
     * @return Map of embedded objects to include in the response.
     */
    public static  ResourceEmbedded processIncludes(Resource resource, FacilityStatusRepository repository,
                                                    List<String> includes) {
        ResourceEmbedded results = new ResourceEmbedded();

        // Check if there are includes to process.
        if (includes != null) {
            // For each specified relationship, find matching links.
            for (String rel : includes) {
                log.debug("[EmbeddedMapping::processIncludes] rel = {}", rel);
                for (Link link : resource.getLinks()) {
                    if (link.getRel().contains(rel)) {
                        Mapping reference = Mapping.getTargetClass(ResourceMapping.RELATIONSHIP_TARGETS, link.getRel());
                        reference.getNamedObject(repository, link.getHref()).ifPresent(o -> {
                            switch (link.getRel()) {
                                case Relationships.MEMBER_OF:
                                    if (results.getMemberOf() == null) {
                                        results.setMemberOf(new ArrayList<>());
                                    }
                                    results.getMemberOf().add((Facility) o);
                                    break;
                                case Relationships.LOCATED_AT:
                                    if (results.getLocatedAt() == null) {
                                        results.setLocatedAt(new ArrayList<>());
                                    }
                                    results.getLocatedAt().add((Site) o);
                                    break;
                                case Relationships.HAS_INCIDENT:
                                    if (results.getHasIncident() == null) {
                                        results.setHasIncident(new ArrayList<>());
                                    }
                                    results.getHasIncident().add((Incident) o);
                                    break;
                                case Relationships.IMPACTED_BY:
                                    if (results.getImpactedBy() == null) {
                                        results.setImpactedBy(new ArrayList<>());
                                    }
                                    results.getImpactedBy().add((Event) o);
                                    break;
                                case Relationships.DEPENDS_ON:
                                    if (results.getDependsOn() == null) {
                                        results.setDependsOn(new ArrayList<>());
                                    }
                                    results.getDependsOn().add((Resource) o);
                                    break;
                                case Relationships.HAS_DEPENDENT:
                                    if (results.getHasDependent() == null) {
                                        results.setHasDependent(new ArrayList<>());
                                    }
                                    results.getHasDependent().add((Resource) o);
                                    break;
                                default:
                                    log.error("[EmbeddedMapping::processIncludes] invalid relationship for resource {}, rel = {}",
                                        resource.getId(), link.getRel());
                                    break;
                            }
                        });
                    }
                }
            }
        }

        return results.isEmpty() ? null : results;
    }

    /**
     * Handles the request to include embedded objects within the Site object.
     *
     * @param site: The Site object that contains the relationships.
     * @param includes: The list of relationships to dereference and add to the embedded property.
     * @return Map of embedded objects to include in the response.
     */
    public static SiteEmbedded processIncludes(Site site, FacilityStatusRepository repository,
                                                List<String> includes) {
        SiteEmbedded results = new SiteEmbedded();

        // Check if there are includes to process.
        if (includes != null) {
            // For each specified relationship, find matching links.
            for (String rel : includes) {
                log.debug("[EmbeddedMapping::processIncludes] rel = {}", rel);
                for (Link link : site.getLinks()) {
                    if (link.getRel().contains(rel)) {
                        Mapping reference = Mapping.getTargetClass(ResourceMapping.RELATIONSHIP_TARGETS, link.getRel());
                        reference.getNamedObject(repository, link.getHref()).ifPresent(o -> {
                            switch (link.getRel()) {
                                case Relationships.HAS_LOCATION:
                                    if (results.getHasLocation() == null) {
                                        results.setHasLocation(new ArrayList<>());
                                    }
                                    results.getHasLocation().add((Location) o);
                                    break;
                                case Relationships.HAS_RESOURCE:
                                    if (results.getHasResource() == null) {
                                        results.setHasResource(new ArrayList<>());
                                    }
                                    results.getHasResource().add((Resource) o);
                                    break;
                                default:
                                    log.error("[EmbeddedMapping::processIncludes] invalid relationship for site {}, rel = {}",
                                        site.getId(), link.getRel());
                                    break;
                            }
                        });
                    }
                }
            }
        }

        return results.isEmpty() ? null : results;
    }

    /**
     * Handles the request to include embedded objects within the Location object.
     *
     * @param location: The Location object that contains the relationships.
     * @param includes: The list of relationships to dereference and add to the embedded property.
     * @return Map of embedded objects to include in the response.
     */
    public static LocationEmbedded processIncludes(Location location, FacilityStatusRepository repository,
                                                   List<String> includes) {
        LocationEmbedded results = new LocationEmbedded();

        // Check if there are includes to process.
        if (includes != null) {
            // For each specified relationship, find matching links.
            for (String rel : includes) {
                log.debug("[EmbeddedMapping::processIncludes] rel = {}", rel);
                for (Link link : location.getLinks()) {
                    if (link.getRel().contains(rel)) {
                        Mapping reference = Mapping.getTargetClass(ResourceMapping.RELATIONSHIP_TARGETS, link.getRel());
                        reference.getNamedObject(repository, link.getHref()).ifPresent(o -> {
                            switch (link.getRel()) {
                                case Relationships.HAS_SITE:
                                    if (results.getHasSite() == null) {
                                        results.setHasSite(new ArrayList<>());
                                    }
                                    results.getHasSite().add((Site) o);
                                    break;
                                default:
                                    log.error("[EmbeddedMapping::processIncludes] invalid relationship for location {}, rel = {}",
                                        location.getId(), link.getRel());
                                    break;
                            }
                        });
                    }
                }
            }
        }

        return results.isEmpty() ? null : results;
    }
}
