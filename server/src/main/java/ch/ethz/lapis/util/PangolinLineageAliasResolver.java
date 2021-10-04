package ch.ethz.lapis.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PangolinLineageAliasResolver {

    private final List<PangolinLineageAlias> aliases;

    public PangolinLineageAliasResolver(List<PangolinLineageAlias> aliases) {
        this.aliases = aliases;
    }

    /**
     * Examples: B.1.1.1.2 -> [C.2] B.1.1.1   -> [C] B.1.1.1*  -> [C.*] B.1.1*    -> [C.*, D.*, K.*, ...] B.1.1.12* ->
     * []
     */
    public List<String> findAlias(String query) {
        // We need to consider the following cases:
        //   Prefix search:
        //     - yes
        //     - no
        //   Query length vs. alias' full name length (The full names of the aliases always have 3 number components):
        //     - Query has less than 3 number components
        //     - Query has at least 3 number components
        //
        // No prefix search + same length/query is longer: 0 or 1 match   -> alias' full name is a prefix of the query
        // No prefix search + query is shorter: 0 match
        // Prefix search    + same length/query is longer: 0 or 1 match   -> alias' full name is a prefix of the query
        // Prefix search    + query is shorter: multiple matches possible -> query is a prefix of the alias' full name

        boolean prefixSearch = query.endsWith("*");
        String queryRoot = query; // The query without a tailing *
        if (prefixSearch) {
            queryRoot = queryRoot.substring(0, queryRoot.length() - 1);
            if (queryRoot.endsWith(".")) {
                queryRoot = queryRoot.substring(0, queryRoot.length() - 1);
            }
        }
        final String finalQueryRoot = queryRoot;
        final boolean queryIsShorter = queryRoot.chars().filter(c -> c == '.').count() < 3;

        if (!queryIsShorter) {
            return aliases.stream()
                .filter(a -> finalQueryRoot.equals(a.getFullName())
                    || finalQueryRoot.startsWith(a.getFullName() + "."))
                .map(a -> a.getAlias() + finalQueryRoot.substring(a.getFullName().length())
                    + (prefixSearch ? ".*" : ""))
                .collect(Collectors.toList());
        }
        if (!prefixSearch && queryIsShorter) {
            return new ArrayList<>();
        }
        if (prefixSearch && queryIsShorter) {
            return aliases.stream()
                .filter(a -> a.getFullName().equals(finalQueryRoot)
                    || a.getFullName().startsWith(finalQueryRoot + "."))
                .map(a -> a.getAlias() + ".*")
                .collect(Collectors.toList());
        }
        throw new RuntimeException("Unexpected error: This should be unreachable. Query: " + query);
    }
}
