package ch.ethz.lapis.api.entity.req;

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

    public String getOrderBy() {
        return orderBy;
    }

    public OrderAndLimitConfig setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public OrderAndLimitConfig setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }
}
