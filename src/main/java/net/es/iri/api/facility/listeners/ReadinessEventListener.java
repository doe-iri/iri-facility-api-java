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
package net.es.iri.api.facility.listeners;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This class is a listener for readiness events.
 *
 * @author hacksaw
 */
@Slf4j
@AllArgsConstructor
@Component
public class ReadinessEventListener {
  private ApplicationAvailability applicationAvailability;

  /**
   * Event listener for the ReadinessState.
   *
   * @param event
   */
  @EventListener
  public void onEvent(AvailabilityChangeEvent<ReadinessState> event) {
    switch (event.getState()) {
      case ACCEPTING_TRAFFIC -> log.info("[ReadinessEventListener] Readiness set to ACCEPTING_TRAFFIC");
      case REFUSING_TRAFFIC -> log.info("[ReadinessEventListener] Readiness set to REFUSING_TRAFFIC");
      default -> log.info("[ReadinessEventListener] Readiness invalid state {}", event.getState());
    }

    log.debug("[ReadinessEventListener] ReadinessState {}, LivenessState {}",
        applicationAvailability.getReadinessState(), applicationAvailability.getLivenessState());
  }
}