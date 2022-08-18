package ch.ethz.lapis.tree;

import java.util.HashMap;
import java.util.Map;

public class SimpleTree {

    private final SimpleTreeNode root;
    private final Map<String, SimpleTreeNode> idIndex;

    public SimpleTree(SimpleTreeNode root) {
        this.root = root;
        idIndex = new HashMap<>();
        root.traverseDFS(node -> idIndex.put(node.getName(), node));
    }
}
