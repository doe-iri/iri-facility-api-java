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
package net.es.iri.api.facility.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApi documentation configuration for IRI Facility API.
 *
 * @author hacksaw
 */
@Slf4j
@Configuration
public class OpenApiConfiguration {

  @Bean
  public OpenAPI customOpenAPI() {
    log.info("Initializing swagger.");
    return new OpenAPI().info(apiInfo());
  }

  private Info apiInfo() {
    return new Info()
        .title("IRI Facility API")
        .description("This API implements the standard integrated research infrastructure specification for facility status.")
        .termsOfService("IRI Facility API Copyright (c) 2025. The Regents of the University of California, through Lawrence Berkeley National Laboratory (subject to receipt of any required approvals from the U.S. Dept. of Energy).  All rights reserved.")
        .version("v1")
        .contact(apiContact())
        .license(apiLicence());
  }

  private License apiLicence() {
    return new License()
        .name("The 3-Clause BSD License")
        .url("https://opensource.org/license/bsd-3-clause/");
  }

  private Contact apiContact() {
    return new Contact()
        .name("The IRI Interfaces Subcommittee")
        .email("software@es.net")
        .url("https://iri.science/ts/interfaces/");
  }
}
