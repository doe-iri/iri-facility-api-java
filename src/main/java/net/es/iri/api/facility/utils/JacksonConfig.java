/*
 *
 *  IRI Facility API reference implementation Copyright (c) 2025,
 *  The Regents of the University of California, through Lawrence
 *  Berkeley National Laboratory (subject to receipt of any required
 *  approvals from the U.S. Dept. of Energy).  All rights reserved.
 *
 *  If you have questions about your rights to use or distribute this
 *  software, please contact Berkeley Lab's Innovation & Partnerships
 *  Office at IPO@lbl.gov.
 *
 *  NOTICE.  This Software was developed under funding from the
 *  U.S. Department of Energy and the U.S. Government consequently retains
 *  certain rights. As such, the U.S. Government has been granted for
 *  itself and others acting on its behalf a paid-up, nonexclusive,
 *  irrevocable, worldwide license in the Software to reproduce,
 *  distribute copies to the public, prepare derivative works, and perform
 *  publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.iri.api.facility.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        // Prevent Jackson from failing on unknown properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Enable parsing of ISO 8601 timestamps with 'Z'
        mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

        return mapper;
    }
}