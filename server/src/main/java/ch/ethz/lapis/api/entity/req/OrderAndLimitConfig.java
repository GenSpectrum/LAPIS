package ch.ethz.lapis.api.entity.req;

import lombok.Data;

@Data
public class OrderAndLimitConfig {

    public static class SpecialOrdering {
        public static final String ARBITRARY = "arbitrary"; // No defined ordering
        public static final String RANDOM = "random";
    }

    /**
     * Allowed values are currently only one of those defined in SpecialOrdering.
     * In the future, we may want to accept a comma-separated list of fields.
     */
    private String orderBy;

    private Integer limit;
}
