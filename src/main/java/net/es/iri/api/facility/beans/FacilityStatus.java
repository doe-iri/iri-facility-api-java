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
package net.es.iri.api.facility.beans;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.iri.api.facility.schema.Event;
import net.es.iri.api.facility.schema.Facility;
import net.es.iri.api.facility.schema.GeographicalLocation;
import net.es.iri.api.facility.schema.Incident;
import net.es.iri.api.facility.schema.Resource;
import net.es.iri.api.facility.schema.Site;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration bean to get IRI Facility Status instance data from application configuration.
 *
 * @author hacksaw
 */
@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "status")
@Validated
public class FacilityStatus {
    // Load from application properties file.
    private List<Facility> facilities = new CopyOnWriteArrayList<>();
    private List<GeographicalLocation> locations = new CopyOnWriteArrayList<>();
    private List<Site> sites = new CopyOnWriteArrayList<>();
    private List<Resource> resources = new CopyOnWriteArrayList<>();
    private List<Incident> incidents = new CopyOnWriteArrayList<>();
    private List<Event> events = new CopyOnWriteArrayList<>();
}
