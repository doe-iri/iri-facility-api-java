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
package net.es.iri.api.facility.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * A utility class for parsing and formatting JSON from annotated objects.
 *
 * @author hacksaw
 */
public class JsonParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static <T> T fromJson(String buffer, Class<T> clazz) {
        try {
            return objectMapper.readValue(buffer, clazz);
        } catch (IOException e) {
            throw new RuntimeException("fromJson: Error reading JSON: " + buffer, e);
        }
    }

    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("toJson: Error writing JSON: " + object.toString(), e);
        }
    }

    /**
     * Deserialize a JSON object from a file into an instance of the specified type.
     *
     * @param filename the path to the JSON file
     * @param clazz    the class of the object
     * @param <T>      the object type
     * @return a deserialized object
     */
    public static <T> T fromFile(String filename, Class<T> clazz) {
        try {
            return objectMapper.readValue(new File(filename), clazz);
        } catch (IOException e) {
            throw new RuntimeException("fromFile: Error reading JSON from file: " + filename, e);
        }
    }

    /**
     * Deserialize a JSON array from a file into a List of the specified type.
     *
     * @param filename the path to the JSON file
     * @param clazz    the class of the array elements
     * @param <T>      the element type
     * @return a List of deserialized objects
     */
    public static <T> List<T> listFromFile(String filename, Class<T> clazz) {
        try {
            CollectionType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, clazz);
            return objectMapper.readValue(new File(filename), listType);
        } catch (IOException e) {
            throw new RuntimeException("listFromFile: Error reading JSON list from file: " + filename, e);
        }
    }
}
