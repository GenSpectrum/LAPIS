package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.api.query.VariantQueryExpr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TreeNode implements VariantQueryExpr {

    private final String label;

    public TreeNode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean[] evaluate(Database database) {
        boolean[] result = new boolean[database.size()];
        var node = database.getTree().getIdIndex().get(label);
        if (node == null) {
            return result;
        }
        // TODO This is now redundant to SimpleTree.getLeaves(). I should find a better place to store the utility
        //  functions.
        List<String> leaves = new ArrayList<>();
        node.traverseBFS(n -> {
            if (n.getChildren().isEmpty()) {
                leaves.add(n.getName());
            }
        });
        Map<String, Integer> gisaidEpiIslToIdMap = database.getGisaidEpiIslToIdMap();
        for (String leaf : leaves) {
            Integer id = gisaidEpiIslToIdMap.get(leaf);
            if (id == null) {
                continue;
            }
            result[id] = true;
        }
        return result;
    }
}
