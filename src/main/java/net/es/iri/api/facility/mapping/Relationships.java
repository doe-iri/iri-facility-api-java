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
package net.es.iri.api.facility.mapping;

/**
 * Constants for modelling link relationships.
 *
 * @author hacksaw
 */
public class Relationships {
    public static final String SELF = "self";
    public static final String HAS_RESOURCE = "hasResource";
    public static final String HAS_EVENT = "hasEvent";
    public static final String GENERATED_BY = "generatedBy";
    public static final String HAS_INCIDENT = "hasIncident";
    public static final String LOCATED_AT = "locatedAt";
    public static final String HOSTED_AT = "hostedAt";
    public static final String HAS_LOCATION = "hasLocation";
    public static final String HAS_SITE = "hasSite";
    public static final String HAS_SUPPORT_URL = "hasSupportURL";
    public static final String IMPACTS = "impacts";
    public static final String IMPACTED_BY = "impactedBy";
    public static final String MAY_IMPACT = "mayImpact";
    public static final String DEPENDS_ON = "dependsOn";
    public static final String HAS_DEPENDENT = "hasDependent";
    public static final String MEMBER_OF = "memberOf";
}
