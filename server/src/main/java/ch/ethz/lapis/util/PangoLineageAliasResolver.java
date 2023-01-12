package ch.ethz.lapis.util;

import java.util.*;
import java.util.stream.Collectors;

public class PangoLineageAliasResolver {

    private final List<PangoLineageAlias> aliases;
    private final Map<String, String> aliasToFullName = new HashMap<>();

    public PangoLineageAliasResolver(List<PangoLineageAlias> aliases) {
        this.aliases = aliases;
        for (PangoLineageAlias alias : aliases) {
            aliasToFullName.put(alias.alias(), alias.fullName());
        }
    }

    /**
     * This function returns aliases of the provided query. It does not return the original query query.
     * <p>
     * Examples:
     * <ul>
     *   <li>B.1.1.1.2 -> [C.2]</li>
     *   <li>B.1.1.1   -> [C]</li>
     *   <li>B.1.1.1*  -> [C.*]</li>
     *   <li>B.1.1*    -> [C.*, D.*, K.*, ..., BA.*, BE.*, ...]</li>
     *   <li>B.1.1.12* -> []</li>
     *   <li>BA.* -> [B.1.1.529*, BE.*, BF.*, ...]</li>
     *   <li>B.1.1.529* -> [BA*, BE.*, BF.*, ...]</li>
     * </ul>
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
        String queryRootFull = queryRoot;
        if (aliasToFullName.containsKey(queryTextComponent)) {
            queryRootFull = queryRootFull.replace(queryTextComponent, aliasToFullName.get(queryTextComponent));
        }
        final String finalQueryRootFull = queryRootFull;

        // The results without tailing * will be collected here:
        List<String> resultRoots = new ArrayList<>();

        // Find the "short version" of the query
        String queryShort = finalQueryRootFull;
        Optional<PangoLineageAlias> queryAliasOpt = aliases.stream()
            .filter(a -> finalQueryRootFull.startsWith(a.fullName() + "."))
            .sorted(Comparator.comparingInt(a -> -a.fullName().length()))
            .findFirst();
        if (queryAliasOpt.isPresent()) {
            PangoLineageAlias alias = queryAliasOpt.get();
            queryShort = queryShort.replace(alias.fullName(), alias.alias());
        }
        if (!queryRoot.equals(queryShort)) { // Exclude original query
            resultRoots.add(queryShort);
        }

        // If no sub lineages need to be included, we are done.
        if (!subLineageSearch) {
            return resultRoots;
        }

        // If sub lineages need to be included, we have to add aliases for which the query is a prefix.
        final String finalQueryRoot = queryRoot;
        List<String> subLineageAliases = aliases.stream()
            .filter(a -> a.fullName().equals(finalQueryRootFull) || a.fullName().startsWith(finalQueryRootFull + "."))
            .map(PangoLineageAlias::alias)
            .filter(a -> !finalQueryRoot.equals(a)) // Exclude original query
            .toList();
        resultRoots.addAll(subLineageAliases);
        return resultRoots.stream().map(s -> s + "*").collect(Collectors.toList());
    }
}
