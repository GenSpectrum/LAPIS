package ch.ethz.lapis.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SimpleTreeNode {

    private String name;
    private SimpleTreeNode parent;
    private final List<SimpleTreeNode> children = new ArrayList<>();

    public SimpleTreeNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SimpleTreeNode setName(String name) {
        this.name = name;
        return this;
    }

    public SimpleTreeNode getParent() {
        return parent;
    }

    public SimpleTreeNode setParent(SimpleTreeNode parent) {
        this.parent = parent;
        return this;
    }

    public List<SimpleTreeNode> getChildren() {
        return children;
    }

    public SimpleTreeNode addChild(SimpleTreeNode child) {
        this.children.add(child);
        return this;
    }

    /**
     * Traverses through the tree in a depth-first fashion and calls the consumer function on every node
     */
    public void traverseDFS(Consumer<SimpleTreeNode> consumer) {
        consumer.accept(this);
        children.forEach(c -> c.traverseDFS(consumer));
    }

}
