package ch.ethz.lapis.source.gisaid;

import org.javatuples.Quartet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This uses geo location rules [1] maintained by Nextstrain to clean up the location data in GISAID. It is implemented
 * similarly to [2].
 *
 * [1] https://github.com/nextstrain/ncov-ingest/blob/master/source-data/gisaid_geoLocationRules.tsv
 * [2] https://github.com/nextstrain/ncov-ingest/blob/04ca33cbed1f96320035b9f7ebcc6abf4fa25a72/lib/utils/transformpipeline/transforms.py
 */
public class GeoLocationMapper {

    private Map<String, Map<String, Map<String, Map<String, String[]>>>> rules = new HashMap<>();

    /**
     * @param geoLocationRulesFile The path to gisaid_geoLocationRules.tsv
     */
    public GeoLocationMapper(Path geoLocationRulesFile) throws IOException {
        this(Files.lines(geoLocationRulesFile).collect(Collectors.toList()));
    }

    /**
     * @param geoLocationRules The content of gisaid_geoLocationRules.tsv
     */
    public GeoLocationMapper(List<String> geoLocationRules) {
        for (String rule : geoLocationRules) {
            String[] leftRight = rule.split("\t");
            if (leftRight.length != 2) {
                throw new RuntimeException(
                        "Unexpected line in gisaid_geoLocationRules.tsv, split by tab failed. Found: " + rule);
            }
            String[] left = leftRight[0].toLowerCase().split("/", -1);
            String[] right = leftRight[1].split("/", -1);
            if (left.length != 4 || right.length != 4) {
                // TODO Temporarily ignoring unexpected rules until https://github.com/nextstrain/ncov-ingest/commit/8518627e8629e9d6cd8b431c742403195c6a94dc#commitcomment-52165696
                //   is clarified.
                continue;
//                throw new RuntimeException(
//                        "Unexpected line in gisaid_geoLocationRules.tsv, split by / failed. Found: " + rule);
            }
            rules.putIfAbsent(left[0], new HashMap<>());
            rules.get(left[0]).putIfAbsent(left[1], new HashMap<>());
            rules.get(left[0]).get(left[1]).putIfAbsent(left[2], new HashMap<>());
            rules.get(left[0]).get(left[1]).get(left[2]).put(left[3], new String[]{
                    right[0], right[1], right[2], right[3]
            });
        }
    }

    private Optional<String[]> findApplicableRule(GeoLocation geoLocation) {
        return findApplicableRule(
                new Quartet<>(geoLocation.getRegion(),
                        geoLocation.getCountry(),
                        geoLocation.getDivision(),
                        geoLocation.getLocation()),
                0, rules);
    }

    private Optional<String[]> findApplicableRule(
            Quartet<String, String, String, String> geoLocation,
            int currentLevel,
            Map<String, ?> currentLevelMap
    ) {
        Object fullMatchValue = currentLevelMap.get(((String) geoLocation.getValue(currentLevel)).toLowerCase());
        Object wildCastValue = currentLevelMap.get("*");
        if (fullMatchValue == null && wildCastValue == null) {
            return Optional.empty();
        }
        if (currentLevel == 3) {
            return Optional.of((String[]) Objects.requireNonNullElse(fullMatchValue, wildCastValue));
        }
        Map<String, ?> fullMatchMap = (Map<String, ?>) fullMatchValue;
        Map<String, ?> wildCastMap = (Map<String, ?>) wildCastValue;
        Map<String, ?> nextLevelMap;
        if (fullMatchMap == null) {
            nextLevelMap = wildCastMap;
        } else if (wildCastMap == null) {
            nextLevelMap = fullMatchMap;
        } else {
            Map<String, Object> _nextLevelMap = new HashMap<>();
            fullMatchMap.forEach((k, v) -> _nextLevelMap.put(k, v));
            wildCastMap.forEach((k, v) -> _nextLevelMap.putIfAbsent(k, v));
            nextLevelMap = _nextLevelMap;
        }
        return findApplicableRule(geoLocation, currentLevel + 1, nextLevelMap);
    }

    public GeoLocation resolve(GeoLocation geoLocation) {
        GeoLocation normalizedGeoLocation = new GeoLocation(
                geoLocation.getRegion() != null ? geoLocation.getRegion().trim() : "",
                geoLocation.getCountry() != null ? geoLocation.getCountry().trim() : "",
                geoLocation.getDivision() != null ? geoLocation.getDivision().trim() : "",
                geoLocation.getLocation() != null ? geoLocation.getLocation().trim() : ""
        );
        GeoLocation resolved = resolve(normalizedGeoLocation, 0);
        return new GeoLocation(
                !"".equals(resolved.getRegion()) ? resolved.getRegion() : null,
                !"".equals(resolved.getCountry()) ? resolved.getCountry() : null,
                !"".equals(resolved.getDivision()) ? resolved.getDivision() : null,
                !"".equals(resolved.getLocation()) ? resolved.getLocation() : null
        );
    }

    private GeoLocation resolve(GeoLocation geoLocation, int numberOfAppliedRules) {
        if (numberOfAppliedRules > 1000) {
            throw new RuntimeException("More than 1000 geographic location rules applied on the same entry. " +
                    "There might be cyclicity in your rules. GeoLocation: " + geoLocation);
        }
        Optional<String[]> ruleOpt = findApplicableRule(geoLocation);
        if (ruleOpt.isEmpty()) {
            return geoLocation;
        }
        String[] rule = ruleOpt.get();
        String[] geoLocationArr = geoLocation.toArray();
        String[] resolved = new String[4];
        for (int i = 0; i < 4; i++) {
            resolved[i] = !"*".equals(rule[i]) ? rule[i] : geoLocationArr[i];
        }
        GeoLocation resolvedGeoLocation = GeoLocation.fromArray(resolved);
        if (geoLocation.equals(resolvedGeoLocation)) {
            return geoLocation;
        }
        return resolve(resolvedGeoLocation, numberOfAppliedRules + 1);
    }
}
