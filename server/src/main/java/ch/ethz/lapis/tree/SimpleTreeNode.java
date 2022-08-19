package ch.ethz.lapis.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class SimpleTreeNode implements Serializable {

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

    /**
     * Traverses through the tree in a breadth-first fashion and calls the consumer function on every node
     */
    public void traverseBFS(Consumer<SimpleTreeNode> consumer) {
        Deque<SimpleTreeNode> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            var node = queue.pollFirst();
            consumer.accept(node);
            for (SimpleTreeNode child : node.children) {
                queue.addLast(child);
            }
        }
    }

}
