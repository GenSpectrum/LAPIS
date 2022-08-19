package ch.ethz.lapis.tree;

import java.util.*;
import java.util.function.Consumer;

public class SimpleTree {

    private SimpleTreeNode root;
    private Map<String, SimpleTreeNode> idIndex;

    public SimpleTree(SimpleTreeNode root) {
        this.root = root;
        constructNewIdIndex();
    }

    /**
     * Traverses through the tree in a depth-first fashion and calls the consumer function on every node
     */
    public void traverseDFS(Consumer<SimpleTreeNode> consumer) {
        root.traverseDFS(consumer);
    }

    /**
     * Traverses through the tree in a breadth-first fashion and calls the consumer function on every node
     */
    public void traverseBFS(Consumer<SimpleTreeNode> consumer) {
        root.traverseBFS(consumer);
    }

    /**
     * Returns a new tree and does not manipulate the current tree.
     *
     * @param names Names of leaves that should be kept
     * @param suppressUnifurcations If true, inner nodes with only one child will be removed.
     */
    public SimpleTree extractTreeWith(Collection<String> names, boolean suppressUnifurcations) {
        return extractTree(names, true, suppressUnifurcations);
    }

    /**
     * Returns a new tree and does not manipulate the current tree.
     *
     * @param names Names of leaves that should be removed
     * @param suppressUnifurcations If true, inner nodes with only one child will be removed.
     */
    public SimpleTree extractTreeWithout(Collection<String> names, boolean suppressUnifurcations) {
        return extractTree(names, false, suppressUnifurcations);
    }

    private void constructNewIdIndex() {
        idIndex = new HashMap<>();
        root.traverseDFS(node -> idIndex.put(node.getName(), node));
    }

    /**
     * Inspired by the implementation in the Python library
     * <a href="https://github.com/niemasd/TreeSwift/blob/master/treeswift/Tree.py">TreeSwift</a>
     */
    private SimpleTree extractTree(Collection<String> names, boolean with, boolean suppressUnifurcations) {
        // The leaves and all parents should be kept (for now)
        Set<String> nodesToKeep = new HashSet<>(names);
        for (String name : names) {
            var node = idIndex.get(name);
            if (node == null) {
                throw new RuntimeException("Node does not exist: " + name);
            }
            while ((node = node.getParent()) != null) {
                nodesToKeep.add(node.getName());
            }
        }
        // Create a tree with only the nodes in nodesToKeep
        SimpleTreeNode newRoot = new SimpleTreeNode(root.getName());
        Deque<SimpleTreeNode> nodeQueue = new LinkedList<>();
        nodeQueue.add(newRoot);
        while (!nodeQueue.isEmpty()) {
            SimpleTreeNode parent = nodeQueue.pop();
            for (SimpleTreeNode child : parent.getChildren()) {
                String childName = child.getName();
                if (nodesToKeep.contains(childName)) {
                    SimpleTreeNode newChildNode = new SimpleTreeNode(childName);
                    parent.addChild(newChildNode);
                    newChildNode.setParent(parent);
                    nodeQueue.add(newChildNode);
                }
            }
        }
        SimpleTree newTree = new SimpleTree(newRoot);
        // Suppress unifurcations if requested
        if (suppressUnifurcations) {
            newTree.suppressUnifurcations();
        }
        // Construct final tree object
        return newTree;
    }

    /**
     * Transform the current tree and remove nodes that have exactly one child
     */
    private void suppressUnifurcations() {
        traverseBFS(node -> {
            if (node.getChildren().size() == 1) {
                var onlyChild = node.getChildren().get(0);
                var parent = node.getParent();
                if (parent == null) {
                    // The child is the new root
                    root = onlyChild;
                    onlyChild.setParent(null);
                } else {
                    parent.getChildren().remove(node);
                    onlyChild.setParent(parent);
                }
            }
        });
    }

    public SimpleTreeNode getRoot() {
        return root;
    }

    public Map<String, SimpleTreeNode> getIdIndex() {
        return idIndex;
    }
}
