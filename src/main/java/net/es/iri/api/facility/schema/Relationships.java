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
package net.es.iri.api.facility.schema;

/**
 * Constants for modelling link relationships.
 *
 * @author hacksaw
 */
public class Relationships {
    public static final String SELF = "self";

    public static final String STATUS_NAMESPACE = "https://schema.doe.gov/iri/facility/status#";

    public static final String HAS_RESOURCE = STATUS_NAMESPACE + "hasResource";
    public static final String HAS_EVENT = STATUS_NAMESPACE + "hasEvent";
    public static final String GENERATED_BY = STATUS_NAMESPACE + "generatedBy";
    public static final String HAS_INCIDENT = STATUS_NAMESPACE + "hasIncident";
    public static final String LOCATED_AT = STATUS_NAMESPACE + "locatedAt";
    public static final String HOSTED_AT = STATUS_NAMESPACE + "hostedAt";
    public static final String HAS_LOCATION = STATUS_NAMESPACE + "hasLocation";
    public static final String HAS_SITE = STATUS_NAMESPACE + "hasSite";
    public static final String SUPPORT_URL = STATUS_NAMESPACE + "supportURL";
    public static final String IMPACTS = STATUS_NAMESPACE + "impacts";
    public static final String IMPACTED_BY = STATUS_NAMESPACE + "impactedBy";
    public static final String MAY_IMPACT = STATUS_NAMESPACE + "mayImpact";
    public static final String DEPENDS_ON = STATUS_NAMESPACE + "dependsOn";
    public static final String HAS_DEPENDENT = STATUS_NAMESPACE + "hasDependent";
    public static final String MEMBER_OF = STATUS_NAMESPACE + "memberOf";
}