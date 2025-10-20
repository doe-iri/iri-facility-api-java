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

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SimulationConfig bean contains properties for loading simulation related resources.
 *
 * @author hacksaw
 */
@Data
@NoArgsConstructor
public class SimulationConfig {
    private int historySize;

    private Status status;

    private Account account;

    @Data
    @NoArgsConstructor
    public static class Status {
        private String facility;
        private String incidents;
        private String events;
        private String locations;
        private String sites;
        private String resources;
    }

    @Data
    @NoArgsConstructor
    public static class Account {
        private String capabilities;
        private String projects;
        private String projectAllocations;
        private String userAllocations;
    }
}
