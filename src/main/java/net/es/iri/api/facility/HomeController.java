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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * A simple controller to redirect the root URL to to the swagger GUI.
 *
 * @author hacksaw
 */
@Slf4j
@Controller
public class HomeController {

    @GetMapping("/")
    public String redirectToSwagger() {
        log.debug("[HomeController::redirectToSwagger] GET operation /");
        return "redirect:/v3/swagger-ui/index.html";
    }
}