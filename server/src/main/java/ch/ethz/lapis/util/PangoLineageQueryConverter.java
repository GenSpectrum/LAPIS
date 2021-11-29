package ch.ethz.lapis.util;

import java.util.ArrayList;
import java.util.List;

public class PangoLineageQueryConverter {

    public class PangoLineageQueryMatch {
        private final List<String> exact;
        private final List<String> prefix;

        public PangoLineageQueryMatch(List<String> exact, List<String> prefix) {
            this.exact = exact;
            this.prefix = prefix;
        }

        public List<String> getExact() {
            return exact;
        }

        public List<String> getPrefix() {
            return prefix;
        }
    }

    private final PangoLineageAliasResolver pangoLineageAliasResolver;


    public PangoLineageQueryConverter(List<PangoLineageAlias> aliases) {
        this.pangoLineageAliasResolver = new PangoLineageAliasResolver(aliases);
    }


    /**
     * This function translates a pangolin lineage query to an array of SQL like-statements. A sequence matches the
     * query if any like-statements are fulfilled. The like-statements are designed to be passed into the following SQL
     * statement: where pangolin_lineage like any(?)
     * <p>
     * Prefix search: Return the lineage and all sub-lineages. I.e., for both "B.1.*" and "B.1*", B.1 and all lineages
     * starting with "B.1." should be returned. "B.11" should not be returned.
     * <p>
     * Example: "B.1.2*" will return [B.1.2, B.1.2.%].
     */
    public String[] convertToSqlLikes(String query) {
        String finalQuery = query.toUpperCase();

        // Resolve aliases
        List<String> resolvedQueries = new ArrayList<>() {{
            add(finalQuery);
        }};
        resolvedQueries.addAll(pangoLineageAliasResolver.findAlias(query));

        // Handle prefix search
        List<String> result = new ArrayList<>();
        for (String resolvedQuery : resolvedQueries) {
            if (resolvedQuery.contains("%")) {
                // Nope, I don't want to allow undocumented features.
            } else if (!resolvedQuery.endsWith("*")) {
                result.add(resolvedQuery);
            } else {
                // Prefix search
                String rootLineage = resolvedQuery.substring(0, resolvedQuery.length() - 1);
                if (rootLineage.endsWith(".")) {
                    rootLineage = rootLineage.substring(0, rootLineage.length() - 1);
                }
                String subLineages = rootLineage + ".%";
                result.add(rootLineage);
                result.add(subLineages);
            }
        }
        return result.toArray(new String[0]);
    }


    public PangoLineageQueryMatch convert(String query) {
        String[] sqlLikes = convertToSqlLikes(query);
        List<String> exact = new ArrayList<>();
        List<String> prefix = new ArrayList<>();
        for (String sqlLike : sqlLikes) {
            if (!sqlLike.endsWith("%")) {
                exact.add(sqlLike);
            } else {
                prefix.add(sqlLike.substring(0, sqlLike.length() - 1));
            }
        }
        return new PangoLineageQueryMatch(exact, prefix);
    }

}
