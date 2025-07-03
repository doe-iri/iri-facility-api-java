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
package net.es.iri.api.facility.beans;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Data;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.Location;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.Site;
import net.es.iri.api.facility.utils.JsonParser;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration bean to get the IRI Facility Status instance data from the application configuration.
 *
 * @author hacksaw
 */
@Data
@Component
@Validated
public class FacilityStatus {
    // Injected configuration.
    private final IriConfig iriConfig;

    // Load from individual files.
    private List<Facility> facilities = new CopyOnWriteArrayList<>();
    private List<Location> locations = new CopyOnWriteArrayList<>();
    private List<Site> sites = new CopyOnWriteArrayList<>();
    private List<Resource> resources = new CopyOnWriteArrayList<>();
    private List<Incident> incidents = new CopyOnWriteArrayList<>();
    private List<Event> events = new CopyOnWriteArrayList<>();

    public FacilityStatus(IriConfig iriConfig) {
        this.iriConfig = iriConfig;

        Optional.ofNullable(iriConfig.getFacility()).ifPresent(file ->
            this.facilities.add(JsonParser.fromFile(file, Facility.class))
        );

        Optional.ofNullable(iriConfig.getLocations()).ifPresent(file ->
            this.locations.addAll(JsonParser.listFromFile(file, Location.class))
        );

        Optional.ofNullable(iriConfig.getSites()).ifPresent(file ->
            this.sites.addAll(JsonParser.listFromFile(file, Site.class))
        );

        Optional.ofNullable(iriConfig.getResources()).ifPresent(file ->
            this.resources.addAll(JsonParser.listFromFile(file, Resource.class))
        );

        Optional.ofNullable(iriConfig.getIncidents()).ifPresent(file ->
            this.incidents.addAll(JsonParser.listFromFile(file, Incident.class))
        );

        Optional.ofNullable(iriConfig.getEvents()).ifPresent(file ->
            this.events.addAll(JsonParser.listFromFile(file, Event.class))
        );
    }
}
