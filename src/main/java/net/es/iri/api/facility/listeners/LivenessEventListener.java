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
package net.es.iri.api.facility.listeners;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This class is invoked when there is a change in application liveness availability.
 *
 * @author hacksaw
 */
@Slf4j
@AllArgsConstructor
@Component
public class LivenessEventListener {
  private ApplicationAvailability applicationAvailability;

  /**
   * Catch an availability event.  Currently we just log the change.
   *
   * @param event
   */
  @EventListener
  public void onEvent(AvailabilityChangeEvent<LivenessState> event) {
    switch (event.getState()) {
      case BROKEN -> log.info("[LivenessEventListener] Liveness start set to BROKEN");
      case CORRECT -> log.info("[LivenessEventListener] Liveness start set to CORRECT");
      default -> log.info("[LivenessEventListener] Liveness invalid state {}", event.getState());
    }

    log.debug("[LivenessEventListener] ReadinessState {}, LivenessState {}",
        applicationAvailability.getReadinessState(), applicationAvailability.getLivenessState());
  }
}