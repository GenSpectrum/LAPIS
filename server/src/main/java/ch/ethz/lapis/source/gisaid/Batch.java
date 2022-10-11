package ch.ethz.lapis.source.gisaid;

import java.util.List;


public record Batch(List<GisaidEntry> entries) {
}
