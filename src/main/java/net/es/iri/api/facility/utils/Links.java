package net.es.iri.api.facility.utils;

import java.util.List;
import net.es.iri.api.facility.schema.Link;
import net.es.iri.api.facility.schema.Relationships;

public class Links {

    public static Link getSelf(List<Link> links) {
        return get(links, Relationships.SELF);
    }

    public static Link getImpactedBy(List<Link> links) {
        return get(links, Relationships.IMPACTED_BY);
    }

    public static Link get(List<Link> links, String relationship) {
        if (links != null) {
            for (Link link : links) {
                if (relationship.equalsIgnoreCase(link.getRel())) {
                    return link;
                }
            }
        }
        return null;
    }
}
