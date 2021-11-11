package ch.ethz.lapis.api.entity.req;

public class OrderAndLimitConfig {

    private Integer limit;

    public Integer getLimit() {
        return limit;
    }

    public OrderAndLimitConfig setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }
}
