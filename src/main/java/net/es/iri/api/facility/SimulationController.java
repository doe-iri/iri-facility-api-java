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

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;
import static java.util.Comparator.reverseOrder;

import java.time.OffsetDateTime;
import java.util.Arrays;
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
import net.es.iri.api.facility.beans.IriConfig;
import net.es.iri.api.facility.datastore.FacilityDataRepository;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.IncidentType;
import net.es.iri.api.facility.schema.ResolutionType;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.ResourceType;
import net.es.iri.api.facility.schema.StatusType;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A simple controller to simulate incidents and events to demo the
 * facility status model.
 *
 * @author hacksaw
 */
@Slf4j
@Component
@Profile("!test") // exclude this component when 'test' profile is active
public class SimulationController {
    // The data store containing facility status information.
    private final FacilityDataRepository repository;

    // Map tracking if a resource is being used for an incident.
    private final Map<ResourceType, Boolean> resourceTypeBeingUsed = new ConcurrentHashMap<>();

    // The root of our absolute URL.
    private final String root;

    private final int historySize;

    /**
     * Constructor of the component.
     */
    public SimulationController(IriConfig iriConfig, FacilityDataRepository repository) {
        // Configuration information specified in application.yaml file.
        this.repository = repository;

        if (iriConfig.getServer() != null) {
            this.root = iriConfig.getServer().getRootOrProxy();
        } else {
            this.root = "https://example.com";
        }

        this.historySize = iriConfig.getSimulation().getHistorySize();
    }

    /**
     * Initialize the initial view of the status-related objects.
     */
    @PostConstruct
    public void init() {
        log.debug("[SimulationController::init] Initializing");

        // Clean up old Incidents.
        pruneIncident();

        // Initialize the map tracking if a resource type is being used for an incident.
        Arrays.stream(ResourceType.values())
            .forEach(t -> resourceTypeBeingUsed.put(t, false));

        // If the resource is not UP then tag the resource group as in use.
        repository.findAllResources().stream()
            .filter(r -> r.getCurrentStatus() != StatusType.UP && r.getType() != null)
            .forEach(r -> resourceTypeBeingUsed.put(r.getType(), true));

        // Dump the initial status.
        resourceTypeBeingUsed.forEach((key, value) -> {
            log.debug("[SimulationController::init] {} : {}", key, value);
        });

        // Populate system start incident and give the all go!
        createStartupIncident();

        // Populate a planned incident.
        getRandomResourceType().ifPresent(g -> {
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
        incident.setName("System startup");
        incident.setDescription("The system has been started.");
        incident.setStatus(StatusType.UP);
        incident.setResolution(ResolutionType.COMPLETED);

        // Link the incident to itself.
        String incidentSelf = incident.self(this.root);
        incident.setSelfUri(incidentSelf);

        // Add the incident to the facility.
        Facility facility = repository.findAllFacilities().getFirst();
        facility.getIncidentUris().add(incidentSelf);
        facility.setLastModified(now);

        // The incident will impact the types resources.
        List<Resource> resources = repository.findAllResources();
        for (Resource r : resources) {
            log.debug("[SimulationController::createStartupIncident] {} : {}", r.getId(), r.getName());

            // Incident mayImpact the Resource.
            incident.getResourceUris().add(r.self(this.root));

            // Create the new down event.
            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            event.setLastModified(now);
            event.setName(r.getName() + " is up");
            event.setDescription("Up event for resource " + r.getId());
            event.setStatus(StatusType.UP);

            // Update resource with new status.
            r.setLastModified(now);
            r.setCurrentStatus(StatusType.UP);

            linkEvent(facility, incident, event, r);

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

            // Pick a resource type to be impacted.
            getRandomResourceType().ifPresentOrElse(t -> {
                    log.debug("[SimulationController::generateIncident] random selection of resourceType: {}", t);

                    // Now we choose between a now incident (90%) and a scheduled maintenance (10%).
                    OffsetDateTime now = OffsetDateTime.now();
                    if (Math.random() < 0.90) {
                        // Generate a unplanned incident.
                        createIncident(IncidentType.UNPLANNED, t, now, now.plusHours((long) (10 * Math.random())));
                    } else {
                        // Generate a scheduled incident.
                        OffsetDateTime startTime = now.plusHours((long) (24 * Math.random()));
                        OffsetDateTime endTime = startTime.plusHours((long) (10 * Math.random()));
                        createIncident(IncidentType.PLANNED, t, startTime, endTime);
                    }
                    resourceTypeBeingUsed.put(t, true);
                }, () -> log.debug("[SimulationController::generateIncident] no resource types available."));
        }
    }

    /**
     * Create an incident.
     *
     * @param type The type of incident.
     * @param resourceType The resourceType of resources the incident applies to.
     * @param startTime The startTime of the incident.
     * @param endTime The endTime of the incident.
     */
    private void createIncident(IncidentType type, ResourceType resourceType, OffsetDateTime startTime,
                                OffsetDateTime endTime) {
        OffsetDateTime now = OffsetDateTime.now();

        // Create a new incident.
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID().toString());
        incident.setType(type);
        incident.setLastModified(now);
        incident.setStart(startTime);
        incident.setEnd(endTime);
        incident.setName(type + " outage on resourceType " + resourceType);
        incident.setDescription("Auto-generated incident of type " + type);
        incident.setStatus(StatusType.DOWN);

        // Link the incident to itself.
        incident.setSelfUri(incident.self(this.root));

        // Add the incident to the facility.
        Facility facility = repository.findAllFacilities().getFirst();
        facility.getIncidentUris().add(incident.self(this.root));
        facility.setLastModified(now);

        // The incident mayImpact resources from this type.
        repository.findAllResources().stream()
            .filter(r -> resourceType ==r.getType())
            .forEach(r -> {
                // Incident mayImpact the Resource.
                incident.getResourceUris().add(r.self(this.root));
            });

        // Handling is a little different for planned and unplanned events.
        if (type == IncidentType.UNPLANNED) {
            // This is a new event to create events for each resource of the impacted type.
            incident.setResolution(ResolutionType.UNRESOLVED);

            List<Resource> resources = repository.findAllResources().stream()
                .filter(r -> resourceType == r.getType())
                .toList();

            for (Resource r :  resources) {
                // Create the new down event.
                Event event = new Event();
                event.setId(UUID.randomUUID().toString());
                event.setLastModified(now);
                event.setName(r.getName() + " is down");
                event.setDescription("Down event for resource " + r.getId());
                event.setStatus(StatusType.DOWN);

                // Update resource with new status.
                r.setLastModified(now);
                r.setCurrentStatus(StatusType.DOWN);

                linkEvent(facility, incident, event, r);

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
    @Scheduled(fixedRate = 30000)     // Runs every 30 seconds
    public void transitionIncidents() {
        log.debug("[SimulationController::transitionIncidents] executed at: {}",
            java.time.LocalDateTime.now());

        Facility facility = repository.findAllFacilities().getFirst();

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
                        transitionToCompleted(facility, incident);
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
                    transitionToCompleted(facility, incident);
                }
            } else if (incident.getResolution() == ResolutionType.PENDING) {
                if (ifPastStartTime(incident)) {
                    log.debug("[SimulationController::transitionIncidents] incident {} : to unresolved",
                        incident.getId());
                    transitionToUnResolved(facility, incident);
                }
            }
        }
    }

    /**
     * Convert an active incident to completed.
     *
     * @param incident The incident to transition to completed.
     */
    public void transitionToCompleted(Facility facility, Incident incident) {
        // We need to get the list of resources associated with this
        // incident and create events.
        OffsetDateTime now = OffsetDateTime.now();
        List<Resource> resources = incident.getResourceUris().stream()
            .filter(Objects::nonNull)
            .map(url -> url.substring(url.lastIndexOf("/") + 1)) // uuid
            .map(repository::findResourceById)
            .toList();

        // We need to create a new event per resource and link it to the incident.
        for (Resource r : resources) {
            log.debug("[SimulationController::transitionToCompleted] processing resource: {}", r.getId());

            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            event.setOccurredAt(now);
            event.setLastModified(now);
            event.setStatus(StatusType.UP);
            event.setName(r.getName());
            event.setDescription(r.getName() + " is UP");

            r.setLastModified(now);
            r.setCurrentStatus(StatusType.UP);

            linkEvent(facility, incident, event, r);

            repository.saveAndFlush(event);
            repository.saveAndFlush(r);
        };

        incident.setLastModified(now);
        incident.setResolution(ResolutionType.COMPLETED);
        repository.saveAndFlush(incident);

        // Remember to reset group status.
        getImpactedGroups(incident).forEach(g -> resourceTypeBeingUsed.put(g, false));
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
    public void transitionToUnResolved(Facility facility, Incident incident) {
        // We need to get the list of resources associated with this
        // incident and create events.
        OffsetDateTime now = OffsetDateTime.now();
        List<Resource> resources = Optional.ofNullable(incident.getResourceUris()).stream()
            .flatMap(List::stream)
            .filter(Objects::nonNull) // href
            .map(url -> url.substring(url.lastIndexOf("/") + 1))
            .map(repository::findResourceById)
            .toList();

        // We need to create a new event per resource and link it to the incident.
        for (Resource r : resources) {
            log.debug("[SimulationController::transitionToUnResolved] processing resource: {}", r.getId());

            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            event.setOccurredAt(now);
            event.setLastModified(now);
            event.setStatus(incident.getStatus());
            event.setName(r.getName());
            event.setDescription(r.getName() + " is " + incident.getStatus());

            r.setLastModified(now);
            r.setCurrentStatus(incident.getStatus());

            linkEvent(facility, incident, event, r);

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
    private void linkEvent(Facility facility, Incident incident, Event event, Resource resource) {
        // Add the event's self link.
        event.setSelfUri(event.self(this.root));

        // Add Event to Facility.
        facility.getEventUris().add(event.getSelfUri());

        // Link the event to the resource.
        event.setResourceUri(resource.self(this.root));

        // Link the incident to the event.
        incident.getEventUris().add(event.self(this.root));

        // The Event is generatedBy an Incident.
        event.setIncidentUri(incident.self(this.root));
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
     * Get the set of resourceTypes associated with the impacted resources from the incident.
     *
     * @param incident The impacting incident.
     * @return A set of ResourceType associated with the impacted resources.
     */
    private Set<ResourceType> getImpactedGroups(Incident incident) {
        return Optional.ofNullable(incident.getResourceUris()).stream()
            .flatMap(List::stream)
            .filter(Objects::nonNull) // href
            .map(url -> url.substring(url.lastIndexOf("/") + 1)) // uuid
            .map(repository::findResourceById)
            .map(Resource::getType)
            .collect(Collectors.toSet());
    }

    /**
     * Get a random resourceType from the list of available resource types.
     *
     * @return A ResourceType.
     */
    private Optional<ResourceType> getRandomResourceType() {
        return resourceTypeBeingUsed.entrySet().stream()
            .filter(entry -> !entry.getValue()) // only entries with value = false
            .map(Map.Entry::getKey) // extract the key
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.isEmpty() ? Optional.empty() :
                    Optional.of(list.get(new Random().nextInt(list.size())))));
    }

    /**
     * Prune old incidents that have ended and exceed the maximum history size.
     * Incidents with null end times are treated as ongoing (OffsetDateTime.MAX) and kept.
     * Incidents are sorted by start time (newest first) and pruned to historySize.
     */
    @Scheduled(fixedRate = 1800000)     // Runs every 30 minutes.
    public void pruneIncident() {
        log.debug("[SimulationController::pruneIncident] historySize: {}, pruning at: {}",
            this.historySize, java.time.LocalDateTime.now());

        OffsetDateTime now = OffsetDateTime.now();
        Facility facility = repository.findAllFacilities().getFirst();

        // Get all incidents that have ended (end time is in the past)
        // Null end times are treated as MAX (ongoing incidents that should be kept).
        List<Incident> endedIncidents = repository.findAllIncidents().stream()
            .filter(incident -> incident.getEnd() != null && incident.getEnd().isBefore(now))
            .sorted(comparing(Incident::getStart, nullsFirst(reverseOrder())))
            .toList();

        log.debug("[SimulationController::pruneIncident] total incidents: {}, ended incidents: {}",
            repository.findAllIncidents().size(), endedIncidents.size());

        // If we have more ended incidents than historySize, prune the excess.
        if (endedIncidents.size() > this.historySize) {
            // Keep the most recent historySize incidents, delete the rest
            List<Incident> incidentsToDelete = endedIncidents.stream()
                .skip(this.historySize)
                .toList();

            log.debug("[SimulationController::pruneIncident] pruning {} incidents",
                incidentsToDelete.size());

            for (Incident incident : incidentsToDelete) {
                log.debug("[SimulationController::pruneIncident] deleting incident: {} ({})",
                    incident.getId(), incident.getName());

                // Remove incident URI from facility
                String incidentUri = incident.self(this.root);
                facility.getIncidentUris().remove(incidentUri);

                // Delete associated events
                List<String> eventUris = Optional.ofNullable(incident.getEventUris())
                    .orElse(List.of());
                
                for (String eventUri : eventUris) {
                    Event event = repository.findEventByHref(eventUri);
                    if (event != null) {
                        // Remove event URI from facility
                        facility.getEventUris().remove(event.getSelfUri());

                        // Delete the event
                        repository.delete(event);
                        log.debug("[SimulationController::pruneIncident] deleted event: {}", event.getId());
                    }
                }

                // Delete the incident
                repository.delete(incident);
            }

            // Save the updated facility
            facility.setLastModified(now);
            repository.saveAndFlush(facility);

            log.debug("[SimulationController::pruneIncident] pruning complete. Remaining incidents: {}",
                repository.findAllIncidents().size());
        } else {
            log.debug("[SimulationController::pruneIncident] no pruning needed");
        }
    }
}
