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
package net.es.iri.api.facility.datastore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.beans.FacilityStatus;
import net.es.iri.api.facility.schema.MediaTypes;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Link;
import net.es.iri.api.facility.schema.Location;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.NamedObject;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.Site;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * FacilityStatusRepository is a data access object (DAO) for managing JSON objects
 * within this demo application. It functions as an in-memory data store for
 * facility status data and related objects such as resources, incidents, events,
 * geographical locations, and more.
 * <p>
 * This implementation is intended for demonstration purposes only and should
 * be replaced by a proper JPA repository and service layer in production.
 * <p>
 * The "id" field should be populated for any save operations, as this
 * implementation expects it to be present for identification purposes.
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class FacilityStatusRepository {
    // Master index for all objects.
    private final ConcurrentMap<String, NamedObject> objects = new ConcurrentHashMap<>();

    // The input facility status model is loaded from an external source.
    private final FacilityStatus facilityStatus;

    /**
     * Constructor to load FacilityStatus bean containing IRI Facility Status instance data.
     *
     * @param facilityStatus The input bean containing IRI Facility Status instance data.
     */
    public FacilityStatusRepository(FacilityStatus facilityStatus) {
        this.facilityStatus = facilityStatus;
    }

    /**
     * Initializes the repository by populating the master index with data
     * from the FacilityStatus bean.
     */
    @PostConstruct
    public void init() {
        log.debug("[FacilityStatusRepository::init] initializing repository.");
        this.facilityStatus.getFacilities().forEach(facility -> {
            fixLinks(facility.getLinks());
            objects.put(facility.getId(), facility);
        });
        this.facilityStatus.getLocations().forEach(location -> {
            fixLinks(location.getLinks());
            objects.put(location.getId(), location);
        });
        this.facilityStatus.getSites().forEach(sites -> {
            fixLinks(sites.getLinks());
            objects.put(sites.getId(), sites);
        });
        this.facilityStatus.getResources().forEach(resource -> {
            fixLinks(resource.getLinks());
            objects.put(resource.getId(), resource);
        });
        this.facilityStatus.getIncidents().forEach(incident -> {
            fixLinks(incident.getLinks());
            objects.put(incident.getId(), incident);
        });
        this.facilityStatus.getEvents().forEach(event -> {
            fixLinks(event.getLinks());
            objects.put(event.getId(), event);
        });
        log.debug("[FacilityStatusRepository::init] repository initialized.");
    }

    private static void fixLinks(List<Link> links) {
        for (Link link : links) {
            if (link.getHref().contains("resources")) {
                link.setType(MediaTypes.RESOURCE);
            } else if (link.getHref().contains("sites")) {
                link.setType(MediaTypes.SITE);
            } else if (link.getHref().contains("locations")) {
                link.setType(MediaTypes.LOCATION);
            } else if (link.getHref().contains("events")) {
                link.setType(MediaTypes.EVENT);
            } else if (link.getHref().contains("incidents")) {
                link.setType(MediaTypes.INCIDENT);
            } else if (link.getHref().contains("facility")) {
                link.setType(MediaTypes.FACILITY);
            }
            else {
                log.error("[FacilityStatusRepository::fixLinks] link type unknown {}", link.getHref());
            }
        }
    }

    /**
     * Saves the given entity to the repository and flushes changes.
     *
     * @param entity The entity to be saved.
     * @param <S> The type of the entity being saved.
     * @return The saved entity.
     */
    public <S extends NamedObject> S saveAndFlush(S entity) {
        objects.put(entity.getId(), entity);
        return entity;
    }

    /**
     * Saves a list of entities to the repository and flushes changes.
     *
     * @param entities The list of entities to be saved.
     * @param <S> The type of the entities being saved.
     * @return The list of saved entities.
     */
    public <S extends NamedObject> List<S> saveAllAndFlush(Iterable<S> entities) {
        List<S> results = new ArrayList<>();
        for (S entity : entities) {
            objects.put(entity.getId(), entity);
        }
        return results;
    }

    /**
     * Deletes all entities in the provided list from the repository.
     *
     * @param entities The entities to delete.
     */
    public void deleteAllInBatch(Iterable<NamedObject> entities) {
        for (NamedObject entity : entities) {
            objects.remove(entity.getId());
        }
    }

    /**
     * Deletes all entities identified by the provided list of IDs.
     *
     * @param strings The list of IDs representing entities to delete.
     */
    public void deleteAllByIdInBatch(Iterable<String> strings) {
        for (String id : strings) {
            objects.remove(id);
        }
    }

    /**
     * Deletes all entities in the repository.
     */
    public void deleteAllInBatch() {
        objects.clear();
    }

    /**
     * Finds and returns an object by its ID.
     *
     * @param s The ID of the object to find.
     * @return The object if found; otherwise, null.
     */
    public NamedObject getOne(String s) {
        return objects.get(s);
    }

    /**
     * Finds and returns an object by its ID.
     *
     * @param s The ID of the object to find.
     * @return The object if found; otherwise, null.
     */
    public NamedObject getById(String s) {
        return objects.get(s);
    }

    /**
     * Finds and returns an optional entity that matches the provided example criteria.
     *
     * @param example The example object containing matching criteria for the search.
     * @param <S> The type of the entity being searched for, which must extend NamedObject.
     * @return An {@code Optional} containing the matching entity if found; otherwise, an empty {@code Optional}.
     */
    public <S extends NamedObject> Optional<S> findOne(Example<S> example) {
        for (NamedObject obj : objects.values()) {
            if (matchesExample(example.getProbe(), obj) && example.getType().isInstance(obj)) {
                return Optional.of(example.getType().cast(obj));
            }
        }
        return Optional.empty();
    }

    /**
     * Determines if two NamedObject instances match based on their properties.
     *
     * @param probe  The reference object containing criteria to match.
     * @param actual The actual object being compared to the probe.
     * @return {@code true} if the `actual` object matches the `probe` criteria; otherwise, {@code false}.
     */
    private boolean matchesExample(NamedObject probe, NamedObject actual) {
        if (probe.getId() != null && !probe.getId().equals(actual.getId())) {
            return false;
        }
        if (probe.getName() != null && !probe.getName().equalsIgnoreCase(actual.getName())) {
            return false;
        }
        if (probe.getShortName() != null && !probe.getShortName().equalsIgnoreCase(actual.getShortName())) {
            return false;
        }
        return probe.getDescription() == null || probe.getDescription().equalsIgnoreCase(actual.getDescription());
    }

    /**
     * Finds and returns a list of entities that match the provided example criteria.
     *
     * @param example The example object containing matching criteria for the search.
     * @param <S> The type of the entity being searched for, which must extend NamedObject.
     * @return A list of matching entities. An empty list is returned if no matches are found.
     */
    @SuppressWarnings("unchecked")
    public <S extends NamedObject> List<S> findAll(Example<S> example) {
        return objects.values().stream()
            .filter(obj -> matchesExample(example.getProbe(), obj))
            .map(obj -> (S) obj)
            .collect(Collectors.toList());
    }

    /**
     * Finds and returns a sorted list of entities that match the provided example criteria.
     *
     * @param example The example object containing matching criteria for the search.
     * @param sort The sorting criteria containing a list of Sort.Order objects specifying sorting rules.
     * @param <S> The type of the entity being searched for, which must extend NamedObject.
     * @return A sorted list of matching entities. An empty list is returned if no matches are found.
     */
    @SuppressWarnings("unchecked")
    public <S extends NamedObject> List<S> findAll(Example<S> example, Sort sort) {
        return objects.values().stream()
            .filter(obj -> matchesExample(example.getProbe(), obj))
            .sorted(getComparator(sort))
            .map(obj -> (S) obj)
            .collect(Collectors.toList());
    }

    /**
     * Generates a comparator for sorting NamedObjects based on provided Sort orders.
     *
     * @param sort The sorting criteria containing a list of Sort.Order objects specifying sorting rules.
     * @return A comparator that sorts NamedObjects based on the given Sort criteria.
     */
    private Comparator<NamedObject> getComparator(Sort sort) {
        Comparator<NamedObject> comparator = Comparator.comparing(obj -> "");

        for (Sort.Order order : sort) {
            Comparator<NamedObject> fieldComparator = Comparator.comparing(obj -> switch (order.getProperty()) {
                case "name" -> obj.getName();
                case "shortName" -> obj.getShortName();
                case "description" -> obj.getDescription();
                case "lastModified" -> obj.getLastModified().toLocalDate().toString();
                default -> obj.getId();  // Default sort by ID
            });

            if (order.isDescending()) {
                fieldComparator = fieldComparator.reversed();
            }

            comparator = comparator.thenComparing(fieldComparator);
        }

        return comparator;
    }

    /**
     * Counts the number of entities that match the provided example criteria.
     *
     * @param example The example object containing matching criteria for the search.
     * @param <S> The type of the entity being searched for, which must extend NamedObject.
     * @return The count of matching entities.
     */
    public <S extends NamedObject> long count(Example<S> example) {
        return objects.values().stream()
            .filter(obj -> matchesExample(example.getProbe(), obj))
            .count();
    }

    /**
     * Determines if there is at least one entity that matches the provided example criteria.
     *
     * @param example The example object containing matching criteria for the search.
     * @param <S> The type of the entity being searched for, which must extend NamedObject.
     * @return {@code true} if a matching entity exists; otherwise, {@code false}.
     */
    public <S extends NamedObject> boolean exists(Example<S> example) {
        return objects.values().stream()
            .anyMatch(obj -> matchesExample(example.getProbe(), obj));
    }

    /**
     * Saves the given entity to the repository.
     *
     * @param entity The entity to be saved.
     * @param <S> The type of the entity being saved.
     * @return The saved entity.
     */
    public <S extends NamedObject> S save(S entity) {
        objects.put(entity.getId(), entity);
        return entity;
    }

    /**
     * Saves all provided entities to the repository.
     *
     * @param entities The entities to be saved.
     * @param <S> The type of the entities being saved, which must extend NamedObject.
     * @return A list of saved entities.
     */
    public <S extends NamedObject> List<S> saveAll(Iterable<S> entities) {
        List<S> results = new ArrayList<>();
        for (S entity : entities) {
            objects.put(entity.getId(), entity);
            results.add(entity);
        }
        return results;
    }

    /**
     * Finds and returns an entity by its ID.
     *
     * @param s The ID of the entity to find.
     * @return An {@code Optional} containing the found entity, or an empty {@code Optional} if no match is found.
     */
    public Optional<NamedObject> findById(String s) {
        return Optional.ofNullable(objects.get(s));
    }

    /**
     * Determines if an entity with the specified ID exists in the repository.
     *
     * @param s The ID of the entity to check.
     * @return {@code true} if an entity with the specified ID exists; otherwise, {@code false}.
     */
    public boolean existsById(String s) {
        return objects.get(s) != null;
    }

    /**
     * Retrieves all entities stored in the repository.
     *
     * @return A list of all stored entities.
     */
    public List<NamedObject> findAll() {
        return new ArrayList<>(objects.values());
    }

    /**
     * Finds and returns a list of entities with the specified IDs.
     *
     * @param strings The list of entity IDs to search for.
     * @return A list of matching entities. An empty list is returned if no matches are found.
     */
    public List<NamedObject> findAllById(Iterable<String> strings) {
        List<NamedObject> results = new ArrayList<>();
        for (String id : strings) {
            Optional.ofNullable(objects.get(id)).ifPresent(results::add);
        }
        return results;
    }

    /**
     * Returns the total number of entities stored in the repository.
     *
     * @return The total count of entities.
     */
    public long count() {
        return objects.size();
    }

    /**
     * Deletes an entity with the specified ID from the repository.
     *
     * @param s The ID of the entity to delete.
     */
    public void deleteById(String s) {
        objects.remove(s);
    }

    /**
     * Deletes the specified entity from the repository.
     *
     * @param entity The entity to delete.
     */
    public void delete(NamedObject entity) {
        objects.remove(entity.getId());
    }

    /**
     * Deletes a list of entities by their IDs from the repository.
     *
     * @param strings The list of entity IDs to delete.
     */
    public void deleteAllById(Iterable<? extends String> strings) {
        for (String id : strings) {
            objects.remove(id);
        }
    }

    /**
     * Deletes all specified entities from the repository.
     *
     * @param entities The entities to delete.
     */
    public void deleteAll(Iterable<? extends NamedObject> entities) {
        for (NamedObject entity : entities) {
            objects.remove(entity.getId());
        }
    }

    /**
     * Deletes all entities from the repository.
     */
    public void deleteAll() {
        objects.clear();
    }

    /**
     * Finds and returns a list of entities sorted according to the provided Sort object.
     *
     * @param sort The sorting criteria specifying the order of sorting.
     * @return A list of sorted entities. If no sorting criteria are provided, an exception is thrown.
     */
    public List<NamedObject> findAll(Sort sort) {
        Comparator<NamedObject> comparator = sort.get()
            .map(order -> {
                Comparator<NamedObject> fieldComparator = Comparator.comparing(object -> switch (order.getProperty()) {
                    case "id" -> object.getId();
                    case "name" -> object.getName();
                    case "shortName" -> object.getShortName();
                    case "lastModified" ->
                        object.getLastModified().toLocalDateTime().toString(); // This will not be properly comparable.
                    default -> throw new IllegalArgumentException("Unknown property: " + order.getProperty());
                }, Comparator.nullsLast(Comparator.naturalOrder()));

                return order.isDescending() ? fieldComparator.reversed() : fieldComparator;
            })
            .reduce(Comparator::thenComparing)
            .orElseThrow(() -> new IllegalArgumentException("No sorting criteria provided"));

        return objects.values().stream().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Find a resource in the global object map based on HREF.
     *
     * @param href The HREF containing the UUID we will use for lookup.
     * @param expectedClass The class derived from NamedObject of the target object.
     * @param <T> The expected type of the resource.
     * @return An instance of the specified class identified by the href, null if not found or is wrong expectedClass.
     */
    @SuppressWarnings("unchecked")
    public <T> T findByHref(String href, Class<T> expectedClass) {
        // Make sure we have a href to lookup.
        if (href != null) {
            // Parse the href for the resource's UUID.
            String uuid = href.substring(href.lastIndexOf("/") + 1);

            // Look up the UUI in the global object map.
            NamedObject obj = objects.get(uuid);

            // Check if the found object is of the expected type
            if (expectedClass.isInstance(obj)) {
                // Return the cast object.
                return (T) obj;
            }
        }
        return null;
    }

    /**
     * Find a resource based on href.
     *
     * @param href The URL reference for the resource to find.
     * @return A facility corresponding to href, or null if not found.
     */
    public Facility findFacilityByHref(String href) {
        return findByHref(href, Facility.class);
    }

    /**
     * Find a resource based on href.
     *
     * @param href The URL reference for the resource to find.
     * @return A resource corresponding to href, or null if not found.
     */
    public Resource findResourceByHref(String href) {
        return findByHref(href, Resource.class);
    }

    /**
     * Find a resource based on href.
     *
     * @param href The URL reference for the resource to find.
     * @return An event corresponding to href, or null if not found.
     */
    public Event findEventByHref(String href) {
        return findByHref(href, Event.class);
    }

    /**
     * Find a resource based on href.
     *
     * @param href The URL reference for the resource to find.
     * @return An incident corresponding to href, or null if not found.
     */
    public Incident findIncidentByHref(String href) {
        return findByHref(href, Incident.class);
    }

    /**
     * Find a resource based on href.
     *
     * @param href The URL reference for the resource to find.
     * @return A location corresponding to href, or null if not found.
     */
    public Location findLocationByHref(String href) {
        return findByHref(href, Location.class);
    }

    /**
     * Return a list of Facilities.
     *
     * @return List of facilities.
     */
    public List<Facility> findAllFacilities() {
        return objects.values().stream()
            .filter(Facility.class::isInstance)
            .map(Facility.class::cast)
            .map(fac -> fac.toBuilder().build())
            .map(Facility.class::cast)
            .toList();
    }

    /**
     * Return Facility matching id.
     *
     * @return The Facility matching id or null.
     */
    public Facility findFacilityById(String id) {
        return Optional.ofNullable(objects.get(id))
            .filter(Facility.class::isInstance)
            .map(Facility.class::cast)
            .map(fac -> fac.toBuilder().build())
            .map(Facility.class::cast)
            .orElse(null);
    }

    /**
     * Return one Facility.
     *
     * @return List of facilities.
     */
    public Facility findOneFacility() {
        return objects.values().stream()
            .filter(Facility.class::isInstance)
            .map(Facility.class::cast)
            .findFirst()
            .map(fac -> fac.toBuilder().build())
            .map(Facility.class::cast)
            .orElse(null);
    }

    /**
     * Return a list of Resources.
     *
     * @return List of resources.
     */
    public List<Resource> findAllResources() {
        return objects.values().stream()
            .filter(Resource.class::isInstance)
            .map(Resource.class::cast)
            .map(res -> res.toBuilder().build())
            .map(Resource.class::cast)
            .toList();
    }

    /**
     * Return Resource matching id.
     *
     * @return The resource matching id or null.
     */
    public Resource findResourceById(String id) {
        return Optional.ofNullable(objects.get(id))
            .filter(Resource.class::isInstance)
            .map(Resource.class::cast)
            .map(res -> res.toBuilder().build())
            .map(Resource.class::cast)
            .orElse(null);
    }

    /**
     * Return a list of Sites.
     *
     * @return List of sites.
     */
    public List<Site> findAllSites() {
        return objects.values().stream()
            .filter(Site.class::isInstance)
            .map(Site.class::cast)
            .map(s -> s.toBuilder().build())
            .map(Site.class::cast)
            .toList();
    }

    /**
     * Return Site matching id.
     *
     * @return The site matching id or null.
     */
    public Site findSiteById(String id) {
        return Optional.ofNullable(objects.get(id))
            .filter(Site.class::isInstance)
            .map(Site.class::cast)
            .map(s -> s.toBuilder().build())
            .map(Site.class::cast)
            .orElse(null);
    }

    /**
     * Return a list of Locations.
     *
     * @return List of locations.
     */
    public List<Location> findAllLocations() {
        return objects.values().stream()
            .filter(Location.class::isInstance)
            .map(Location.class::cast)
            .map(g -> g.toBuilder().build())
            .map(Location.class::cast)
            .toList();
    }

    /**
     * Return GeographicalLocation matching id.
     *
     * @return The location matching id or null.
     */
    public Location findLocationById(String id) {
        return Optional.ofNullable(objects.get(id))
            .filter(Location.class::isInstance)
            .map(Location.class::cast)
            .map(g -> g.toBuilder().build())
            .map(Location.class::cast)
            .orElse(null);
    }

    /**
     * Return a list of Incidents.
     *
     * @return List of incidents.
     */
    public List<Incident> findAllIncidents() {
        return objects.values().stream()
            .filter(Incident.class::isInstance)
            .map(Incident.class::cast)
            .map(i -> i.toBuilder().build())
            .map(Incident.class::cast)
            .toList();
    }

    /**
     * Return Incident matching id.
     *
     * @return The incident matching id or null.
     */
    public Incident findIncidentById(String id) {
        return Optional.ofNullable(objects.get(id))
            .filter(Incident.class::isInstance)
            .map(Incident.class::cast)
            .map(i -> i.toBuilder().build())
            .map(Incident.class::cast)
            .orElse(null);
    }

    /**
     * Return a list of Events.
     *
     * @return List of events.
     */
    public List<Event> findAllEvents() {
        return objects.values().stream()
            .filter(Event.class::isInstance)
            .map(Event.class::cast)
            .map(e -> e.toBuilder().build())
            .map(Event.class::cast)
            .toList();
    }

    /**
     * Return Event matching id.
     *
     * @return The event matching id or null.
     */
    public Event findEventById(String id) {
        return Optional.ofNullable(objects.get(id))
            .filter(Event.class::isInstance)
            .map(Event.class::cast)
            .map(e -> e.toBuilder().build())
            .map(Event.class::cast)
            .orElse(null);
    }

}
