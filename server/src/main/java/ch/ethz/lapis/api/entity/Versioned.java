package ch.ethz.lapis.api.entity;


public record Versioned<T>(long dataVersion, T content) {
}
