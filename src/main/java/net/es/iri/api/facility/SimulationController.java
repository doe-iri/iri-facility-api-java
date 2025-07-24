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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.iri.api.facility.datastore.FacilityStatusRepository;
import net.es.iri.api.facility.schema.MediaTypes;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.IncidentType;
import net.es.iri.api.facility.schema.Link;
import net.es.iri.api.facility.schema.Relationships;
import net.es.iri.api.facility.schema.ResolutionType;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.StatusType;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A simple controller to simulated incidents and events to demo the
 * facility status model.
 *
 * @author hacksaw
 */
@Slf4j
@Component
@Profile("!test") // exclude this component when 'test' profile is active
public class SimulationController {
    // The data store containing facility status information.
    private final FacilityStatusRepository repository;

    // Map tracking if a resource group is being used for an incident.
    private final Map<String, Boolean> groupBeingUsed = new ConcurrentHashMap<>();

    /**
     * Constructor of the component.
     *
     * @param repository
     */
    public SimulationController(FacilityStatusRepository repository) {
        this.repository = repository;
    }

    /**
     * Initialize the initial view of the status-related objects.
     */
    @PostConstruct
    public void init() {
        log.debug("[SimulationController::init] Initializing");

        // Initialize the map tracking if a resource group is being used for an incident.
        repository.findAllResources().stream()
            .map(Resource::getGroup)
            .collect(Collectors.toSet())
            .forEach(g -> groupBeingUsed.put(g, false));

        // If the resource is not UP then tag the resource group as in use.
        repository.findAllResources().stream()
            .filter(r -> r.getCurrentStatus() != StatusType.UP && r.getGroup() != null && !r.getGroup().isEmpty())
            .forEach(r -> groupBeingUsed.put(r.getGroup(), true));

        // Dump the initial status.
        groupBeingUsed.forEach((key, value) -> {
            log.debug("[SimulationController::init] {} : {}", key, value);
        });

        // Populate system start incident and give the all go!
        createStartupIncident();

        // Populate a planned incident.
        getRandomGroup().ifPresent(g -> {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime startTime = now.plusHours((long) (24 * Math.random()));
            OffsetDateTime endTime = startTime.plusHours((long) (10 * Math.random()));
            createIncident(IncidentType.PLANNED, g, startTime, endTime);
        });
    }

    /**
     * Create a system up incident and associated events.
     */
    private void createStartupIncident() {
        OffsetDateTime now = OffsetDateTime.now();

        // Create a new incident.
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID().toString());
        incident.setType(IncidentType.UNPLANNED);
        incident.setLastModified(now);
        incident.setStart(now);
        incident.setEnd(now);
        incident.setShortName("Startup");
        incident.setName("System startup");
        incident.setDescription("The system has been started.");
        incident.setStatus(StatusType.UP);
        incident.setResolution(ResolutionType.COMPLETED);

        // Link the incident to itself.
        Link self = Link.builder()
            .rel(Relationships.SELF)
            .href(String.format(Incident.URL_TEMPLATE, incident.getId()))
            .type(MediaTypes.INCIDENT)
            .build();
        incident.getLinks().add(self);

        // Add the incident to the facility.
        Facility facility = repository.findAllFacilities().getFirst();
        Link hasIncident = Link.builder()
            .rel(Relationships.HAS_INCIDENT)
            .href(String.format(Incident.URL_TEMPLATE, incident.getId()))
            .type(MediaTypes.INCIDENT)
            .build();
        facility.getLinks().add(hasIncident);
        facility.setLastModified(now);

        // The incident will impact the group's resources.
        List<Resource> resources = repository.findAllResources();
        for (Resource r : resources) {
            // Remove all incidents referenced by the resource that are no
            // longer active, leaving only the active and future incidents.
            List<Link> links = new ArrayList<>();
            for (Link link : r.getLinks()) {
                if (link.getRel().equalsIgnoreCase(Relationships.HAS_INCIDENT)) {
                    Optional.ofNullable(repository.findIncidentByHref(link.getHref())).ifPresent(i -> {
                        if (i.getEnd() != null && OffsetDateTime.now().isAfter(i.getEnd())) {
                            links.add(link);
                        }
                    });
                }
            }
            r.getLinks().removeAll(links);

            // Incident mayImpact the Resource.
            Link mayImpact = Link.builder()
                .rel(Relationships.MAY_IMPACT)
                .href(String.format(Resource.URL_TEMPLATE, r.getId()))
                .type(MediaTypes.RESOURCE)
                .build();
            incident.getLinks().add(mayImpact);

            // A Resource hasIncident.
            r.getLinks().add(hasIncident);

            // Create the new down event.
            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            event.setLastModified(now);
            event.setName(r.getShortName() + " is up");
            event.setShortName(r.getShortName());
            event.setDescription("Up event for resource " + r.getId());
            event.setStatus(StatusType.UP);

            // Update resource with new status.
            r.setLastModified(now);
            r.setCurrentStatus(StatusType.UP);

            linkEvent(incident, event, r);

            // Save what we have modified here.
            repository.saveAndFlush(event);
            repository.saveAndFlush(r);
        }

        // Save the new incident and update the facility accordingly.
        repository.saveAndFlush(incident);
        repository.saveAndFlush(facility);
    }

    /**
     * Autonomously generate a new incident based on a timed event and probability.
     */
    @Scheduled(fixedRate = 1800000)     // Runs every 30 minutes.
    public void generateIncident() {
        log.debug("[SimulationController::generateIncident] executed at: {}",
            java.time.LocalDateTime.now());

        // There is a 10% chance of an incident occurring.
        double random = Math.random();
        if (random < 0.10) {
            log.debug("[SimulationController::generateIncident] creating an incident.");

            // Pick a group to be impacted.
            getRandomGroup().ifPresentOrElse(g -> {
                    log.debug("[SimulationController::generateIncident] random selection of group: {}", g);

                    // Now we choose between a now incident (90%) and a scheduled maintenance (10%).
                    OffsetDateTime now = OffsetDateTime.now();
                    if (Math.random() < 0.90) {
                        // Generate a unplanned incident.
                        createIncident(IncidentType.UNPLANNED, g, now, now.plusHours((long) (10 * Math.random())));
                    } else {
                        // Generate a scheduled incident.
                        OffsetDateTime startTime = now.plusHours((long) (24 * Math.random()));
                        OffsetDateTime endTime = startTime.plusHours((long) (10 * Math.random()));
                        createIncident(IncidentType.PLANNED, g, startTime, endTime);
                    }
                    groupBeingUsed.put(g, true);
                }, () -> log.debug("[SimulationController::generateIncident] no groups available."));
        }
    }

    /**
     * Create an incident.
     *
     * @param type The type of incident.
     * @param group The group of resources the incident applies to.
     * @param startTime The startTime of the incident.
     * @param endTime The endTime of the incident.
     */
    private void createIncident(IncidentType type, String group, OffsetDateTime startTime, OffsetDateTime endTime) {
        OffsetDateTime now = OffsetDateTime.now();

        // Create a new incident.
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID().toString());
        incident.setType(type);
        incident.setLastModified(now);
        incident.setStart(startTime);
        incident.setEnd(endTime);
        incident.setShortName(group);
        incident.setName(type + " outage on group " + group);
        incident.setDescription("Auto-generated incident of type " + type);
        incident.setStatus(StatusType.DOWN);

        // Link the incident to itself.
        Link self = Link.builder()
            .rel(Relationships.SELF)
            .href(String.format(Incident.URL_TEMPLATE, incident.getId()))
            .type(MediaTypes.INCIDENT)
            .build();
        incident.getLinks().add(self);

        // Add the incident to the facility.
        Facility facility = repository.findAllFacilities().getFirst();
        Link hasIncident = Link.builder()
            .rel(Relationships.HAS_INCIDENT)
            .href(String.format(Incident.URL_TEMPLATE, incident.getId()))
            .type(MediaTypes.INCIDENT)
            .build();
        facility.getLinks().add(hasIncident);
        facility.setLastModified(now);

        // The incident mayImpact resources from group.
        repository.findAllResources().stream()
            .filter(r -> group.equalsIgnoreCase(r.getGroup()))
            .forEach(r -> {
                // Incident mayImpact the Resource.
                Link mayImpact = Link.builder()
                    .rel(Relationships.MAY_IMPACT)
                    .href(String.format(Resource.URL_TEMPLATE, r.getId()))
                    .type(MediaTypes.RESOURCE)
                    .build();
                incident.getLinks().add(mayImpact);

                // A Resource hasIncident to Incident.
                incident.getLinks().add(hasIncident);
            });

        // Handling is a little different for planned and unplanned events.
        if (type == IncidentType.UNPLANNED) {
            // This is a new event to create events for each resource in the impacted group.
            incident.setResolution(ResolutionType.UNRESOLVED);

            List<Resource> resources = repository.findAllResources().stream()
                .filter(r -> group.equalsIgnoreCase(r.getGroup()))
                .toList();

            for (Resource r :  resources) {
                // Create the new down event.
                Event event = new Event();
                event.setId(UUID.randomUUID().toString());
                event.setLastModified(now);
                event.setName(r.getShortName() + " is down");
                event.setShortName(r.getShortName());
                event.setDescription("Down event for resource " + r.getId());
                event.setStatus(StatusType.DOWN);

                // Update resource with new status.
                r.setLastModified(now);
                r.setCurrentStatus(StatusType.DOWN);

                linkEvent(incident, event, r);

                // Save what we have modified here.
                repository.saveAndFlush(event);
                repository.saveAndFlush(r);
            }
        } else {
            // This is a now event, so no events needed.
            incident.setResolution(ResolutionType.PENDING);
        }

        // Save the new incident and update the facility accordingly.
        repository.saveAndFlush(incident);
        repository.saveAndFlush(facility);
    }

    /**
     * A scheduled transition of an incident through the lifecycle.
     */
    @Scheduled(fixedRate = 5000)     // Runs every 5 seconds
    public void transitionIncidents() {
        log.debug("[SimulationController::transitionIncidents] executed at: {}",
            java.time.LocalDateTime.now());

        // Care and feeding for existing incidents.
        double random = Math.random();
        List<Incident> incidents = repository.findAllIncidents().stream()
            .filter(i -> i.getResolution() != ResolutionType.COMPLETED)
            .toList();

        for (Incident incident : incidents) {
            log.debug("[SimulationController::transitionIncidents] processing incident {} : {}",
                incident.getId(), incident.getResolution());
            if (incident.getResolution() == ResolutionType.UNRESOLVED) {
                // Two options - extend the incident (5%) or resolve it (95%).
                if (ifPastEndTime(incident)) {
                    log.debug("[SimulationController::transitionIncidents] unresolved incident {} : past endTime",
                        incident.getId());
                    if (random < 0.90) {
                        log.debug("[SimulationController::transitionIncidents] incident {} : to completed",
                            incident.getId());
                        transitionToCompleted(incident);
                    } else {
                        log.debug("[SimulationController::transitionIncidents] incident {} : to extended",
                            incident.getId());
                        transitionToExtended(incident);
                    }
                }
            } else if (incident.getResolution() == ResolutionType.EXTENDED) {
                if (ifPastEndTime(incident)) {
                    log.debug("[SimulationController::transitionIncidents] incident {} : to completed",
                        incident.getId());
                    transitionToCompleted(incident);
                }
            } else if (incident.getResolution() == ResolutionType.PENDING) {
                if (ifPastStartTime(incident)) {
                    log.debug("[SimulationController::transitionIncidents] incident {} : to unresolved",
                        incident.getId());
                    transitionToUnResolved(incident);
                }
            }
        }
    }

    /**
     * Convert an active incident to completed.
     *
     * @param incident The incident to transition to completed.
     */
    public void transitionToCompleted(Incident incident) {
        // We need to get the list of resources associated with this
        // incident and create events.
        OffsetDateTime now = OffsetDateTime.now();
        List<Resource> resources = incident.getLinks().stream()
            .filter(l -> Relationships.IMPACTS.equalsIgnoreCase(l.getRel()))
            .map(Link::getHref)
            .filter(Objects::nonNull) // href
            .map(url -> url.substring(url.lastIndexOf("/") + 1)) // uuid
            .map(repository::findResourceById)
            .toList();

        for (Resource r : resources) {
            // We need to create a new event per resource and link it to the incident.
            log.debug("[SimulationController::transitionToCompleted] processing resource: {}", r.getId());

            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            event.setOccurredAt(now);
            event.setLastModified(now);
            event.setStatus(StatusType.UP);
            event.setName(r.getName());
            event.setShortName(r.getShortName());
            event.setDescription(r.getShortName() + " is UP");

            r.setLastModified(now);
            r.setCurrentStatus(StatusType.UP);

            linkEvent(incident, event, r);

            repository.saveAndFlush(event);
            repository.saveAndFlush(r);
        };

        incident.setLastModified(now);
        incident.setResolution(ResolutionType.COMPLETED);
        repository.saveAndFlush(incident);

        // Remember to reset group status.
        getImpactedGroups(incident).forEach(g -> groupBeingUsed.put(g, false));
    }

    /**
     * Convert an active incident to an extended one.
     *
     * @param incident The incident to transition to extended.
     */
    public void transitionToExtended(Incident incident) {
        incident.setEnd(OffsetDateTime.now().plusHours(1));
        incident.setResolution(ResolutionType.EXTENDED);
        incident.setLastModified(OffsetDateTime.now());
        repository.saveAndFlush(incident);
    }

    /**
     * Convert a pending incident to unresolved.
     *
     * @param incident The incident to transition to unresolved.
     */
    public void transitionToUnResolved(Incident incident) {
        // We need to get the list of resources associated with this
        // incident and create events.
        OffsetDateTime now = OffsetDateTime.now();
        List<Resource> resources = Optional.ofNullable(incident.getLinks()).stream()
            .flatMap(List::stream)
            .filter(l -> Relationships.IMPACTS.equalsIgnoreCase(l.getRel()))
            .map(Link::getHref)
            .filter(Objects::nonNull) // href
            .map(url -> url.substring(url.lastIndexOf("/") + 1))
            .map(repository::findResourceById)
            .toList();

        for (Resource r : resources) {
            // We need to create a new event per resource and link it to the incident.
            log.debug("[SimulationController::transitionToUnResolved] processing resource: {}", r.getId());

            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            event.setOccurredAt(now);
            event.setLastModified(now);
            event.setStatus(incident.getStatus());
            event.setName(r.getName());
            event.setShortName(r.getShortName());
            event.setDescription(r.getShortName() + " is " + incident.getStatus());

            r.setLastModified(now);
            r.setCurrentStatus(incident.getStatus());

            linkEvent(incident, event, r);

            repository.saveAndFlush(event);
            repository.saveAndFlush(r);
        }

        incident.setLastModified(now);
        incident.setResolution(ResolutionType.UNRESOLVED);
        repository.saveAndFlush(incident);
    }

    /**
     * Create all the relationships needed for an event.
     *
     * @param incident The incident associated with the event.
     * @param event The event to be linked.
     * @param resource The resource impacted by the event.
     */
    private void linkEvent(Incident incident, Event event, Resource resource) {
        // Add the event's self link.
        Link self = Link.builder()
            .rel(Relationships.SELF)
            .href(String.format(Event.URL_TEMPLATE, event.getId()))
            .type(MediaTypes.EVENT)
            .build();
        event.getLinks().add(self);

        // Link the event to the resource.
        Link impacts = Link.builder()
            .rel(Relationships.IMPACTS)
            .href(String.format(Resource.URL_TEMPLATE, resource.getId()))
            .type(MediaTypes.RESOURCE)
            .build();
        event.getLinks().add(impacts);

        // A Resource is impactedBy the last Event generated, so we need to
        // remove any impactedBy links already in place.
        List<Link> remove = new ArrayList<>();
        for (Link link : resource.getLinks()) {
            if (link.getRel().equals(Relationships.IMPACTED_BY)) {
                remove.add(link);
            }
        }
        resource.getLinks().removeAll(remove);

        // Add this new impactedBy Event.
        Link impactedBy = Link.builder()
            .rel(Relationships.IMPACTED_BY)
            .href(String.format(Event.URL_TEMPLATE, event.getId()))
            .type(MediaTypes.EVENT)
            .build();
        resource.getLinks().add(impactedBy);

        // Link the incident to the event.
        Link hasEvent = Link.builder()
            .rel(Relationships.HAS_EVENT)
            .href(String.format(Event.URL_TEMPLATE, event.getId()))
            .type(MediaTypes.EVENT)
            .build();
        incident.getLinks().add(hasEvent);

        // The Event is generatedBy an Incident.
        Link generatedBy = Link.builder()
            .rel(Relationships.GENERATED_BY)
            .href(String.format(Incident.URL_TEMPLATE, incident.getId()))
            .type(MediaTypes.INCIDENT)
            .build();
        event.getLinks().add(generatedBy);
    }

    /**
     * Is this time more than a day old?
     *
     * @param dateTime The date to check for age.
     * @return True if the date is older than a day from now.
     */
    public static boolean isMoreThanOneDayOld(OffsetDateTime dateTime) {
        OffsetDateTime oneDayAgo = OffsetDateTime.now().minusDays(1);
        return dateTime.isBefore(oneDayAgo);
    }

    /**
     * Is this incident past the scheduled endTime?
     *
     * @param incident The incident to check.
     * @return True if the incident is past the scheduled endTime.
     */
    public static boolean ifPastEndTime(Incident incident) {
        return ((incident.getEnd() == null && isMoreThanOneDayOld(incident.getLastModified()))) ||
            OffsetDateTime.now().isAfter(incident.getEnd());
    }

    /**
     * Is this incident past the scheduled startTime?
     *
     * @param incident The incident to check.
     * @return True if the incident is past the scheduled startTime.
     */
    public static boolean ifPastStartTime(Incident incident) {
        return incident.getStart() == null || OffsetDateTime.now().isAfter(incident.getStart());
    }

    /**
     * Get the set of groups associated with the impacted resources from the incident.
     *
     * @param incident The impacting incident.
     * @return A set of group names associated with the impacted resources.
     */
    private Set<String> getImpactedGroups(Incident incident) {
        return Optional.ofNullable(incident.getLinks()).stream()
            .flatMap(List::stream)
            .filter(l -> Relationships.IMPACTS.equalsIgnoreCase(l.getRel()))
            .map(Link::getHref)
            .filter(Objects::nonNull) // href
            .map(url -> url.substring(url.lastIndexOf("/") + 1)) // uuid
            .map(repository::findResourceById)
            .map(Resource::getGroup)
            .collect(Collectors.toSet());
    }

    /**
     * Get a random group from the list of available resource groups.
     *
     * @return A resource group name.
     */
    private Optional<String> getRandomGroup() {
        return groupBeingUsed.entrySet().stream()
            .filter(entry -> !entry.getValue()) // only entries with value = false
            .map(Map.Entry::getKey) // extract the key
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.isEmpty() ? Optional.empty() :
                    Optional.of(list.get(new Random().nextInt(list.size())))))
            .map(Object::toString);
    }
}
