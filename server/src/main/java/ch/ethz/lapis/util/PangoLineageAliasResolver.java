package ch.ethz.lapis.util;

import java.util.*;
import java.util.stream.Collectors;

public class PangoLineageAliasResolver {

    private final List<PangoLineageAlias> aliases;
    private final Map<String, String> aliasToFullName = new HashMap<>();

    public PangoLineageAliasResolver(List<PangoLineageAlias> aliases) {
        this.aliases = aliases;
        for (PangoLineageAlias alias : aliases) {
            aliasToFullName.put(alias.getAlias(), alias.getFullName());
        }
    }

    /**
     * Examples:
     *   - B.1.1.1.2 -> [C.2]
     *   - B.1.1.1   -> [C]
     *   - B.1.1.1*  -> [C.*]
     *   - B.1.1*    -> [C.*, D.*, K.*, ..., BA.*, BE.*, ...]
     *   - B.1.1.12* -> []
     *   - BA.* -> [BE.*, BF.*, ...]
     */
    public List<String> findAlias(String query) {
        // Normalize/parse the query
        boolean subLineageSearch = query.endsWith("*");
        String queryRoot = query.toUpperCase(); // The query without a tailing * and upper-case
        if (subLineageSearch) {
            queryRoot = queryRoot.substring(0, queryRoot.length() - 1);
            if (queryRoot.endsWith(".")) {
                queryRoot = queryRoot.substring(0, queryRoot.length() - 1);
            }
        }

        // Find the full name of the query
        String queryTextComponent = queryRoot.split("\\.")[0];
        if (aliasToFullName.containsKey(queryTextComponent)) {
            queryRoot = queryRoot.replace(queryTextComponent, aliasToFullName.get(queryTextComponent));
        }
        String finalQueryRoot = queryRoot;

        // The results without tailing * will be collected here:
        List<String> resultRoots = new ArrayList<>();

        // Find the "short version" of the query
        String queryShort = finalQueryRoot;
        Optional<PangoLineageAlias> queryAliasOpt = aliases.stream()
            .filter(a -> finalQueryRoot.startsWith(a.getFullName() + "."))
            .sorted(Comparator.comparingInt(a -> -a.getFullName().length()))
            .findAny();
        if (queryAliasOpt.isPresent()) {
            PangoLineageAlias alias = queryAliasOpt.get();
            queryShort = queryShort.replace(alias.getFullName(), alias.getAlias());
        }
        if (!queryRoot.equals(queryShort)) {
            resultRoots.add(queryShort);
        }

        // If no sub lineages need to be included, we are done.
        if (!subLineageSearch) {
            return resultRoots;
        }

        // If sub lineages need to be included, we have to add aliases for which the query is a prefix.
        List<String> subLineageAliases = aliases.stream()
            .filter(a -> a.getFullName().equals(finalQueryRoot) || a.getFullName().startsWith(finalQueryRoot + "."))
            .map(PangoLineageAlias::getAlias)
            .collect(Collectors.toList());
        resultRoots.addAll(subLineageAliases);
        return resultRoots.stream().map(s -> s + "*").collect(Collectors.toList());
    }
}
