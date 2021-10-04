package ch.ethz.lapis.api.entity;

public enum OpennessLevel {

    /**
     * The underlying dataset can be fully shared.
     */
    OPEN,

    /**
     * Only aggregated data may be shared. Sequences may not be shared.
     */
    GISAID

}
