package ch.ethz.lapis.tree;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SimpleTreeTest {

    private SimpleTree getTree0() {
        // - node 0
        //     - node 1
        //         - node 2
        //         - node 3
        //     - node 4
        //         - node 5
        //         - node 6
        //         - node 7
        //             - node 8
        //             - node 9
        //             - node 10
        List<SimpleTreeNode> nodes = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            nodes.add(new SimpleTreeNode(String.valueOf(i)));
        }
        nodes.get(0).addChild(nodes.get(1)).addChild(nodes.get(4));
        nodes.get(1).setParent(nodes.get(0)).addChild(nodes.get(2)).addChild(nodes.get(3));
        nodes.get(2).setParent(nodes.get(1));
        nodes.get(3).setParent(nodes.get(1));
        nodes.get(4).setParent(nodes.get(0)).addChild(nodes.get(5)).addChild(nodes.get(6)).addChild(nodes.get(7));
        nodes.get(5).setParent(nodes.get(4));
        nodes.get(6).setParent(nodes.get(4));
        nodes.get(7).setParent(nodes.get(4)).addChild(nodes.get(8)).addChild(nodes.get(9)).addChild(nodes.get(10));
        nodes.get(8).setParent(nodes.get(7));
        nodes.get(9).setParent(nodes.get(7));
        nodes.get(10).setParent(nodes.get(7));
        return new SimpleTree(nodes.get(0));
    }

    @Test
    public void testTraverseBFS() {
        var tree = getTree0();
        List<String> order = new ArrayList<>();
        tree.traverseBFS(n -> order.add(n.getName()));
        Assertions.assertEquals("0,1,4,2,3,5,6,7,8,9,10", String.join(",", order));
    }

    @Test
    public void testTraverseDFS() {
        var tree = getTree0();
        List<String> order = new ArrayList<>();
        tree.traverseDFS(n -> order.add(n.getName()));
        Assertions.assertEquals("0,1,2,3,4,5,6,7,8,9,10", String.join(",", order));
    }

    @Test
    public void testGetLeaves() {
        var tree = getTree0();
        var leaves = new HashSet<>(tree.getLeaves());
        var expected = new HashSet<>(List.of("2", "3", "5", "6", "8", "9", "10"));
        Assertions.assertEquals(expected, leaves);
    }

    @Test
    public void testExtractTreeWith() {
        var tree = getTree0();
        var subTreeWithUnifurcations = tree.extractTreeWith(List.of("5", "9", "10"), false);
        Assertions.assertEquals("0,4,5,7,9,10", treeToBFSNodeList(subTreeWithUnifurcations));
        var subTreeWithoutUnifurcations = tree.extractTreeWith(List.of("5", "9", "10"), true);
        Assertions.assertEquals("4,5,7,9,10", treeToBFSNodeList(subTreeWithoutUnifurcations));
    }

    @Test
    public void testExtractTreeWithout() {
        var tree = getTree0();
        var subTreeWithUnifurcations = tree.extractTreeWithout(List.of("5", "9", "10"), false);
        Assertions.assertEquals("0,1,4,2,3,6,7,8", treeToBFSNodeList(subTreeWithUnifurcations));
        var subTreeWithoutUnifurcations = tree.extractTreeWithout(List.of("5", "9", "10"), true);
        Assertions.assertEquals("0,1,4,2,3,6,8", treeToBFSNodeList(subTreeWithoutUnifurcations));
    }

    /**
     * As long as no proper equals() is implemented, this function will be used to compare the equality of two trees.
     */
    private String treeToBFSNodeList(SimpleTree tree) {
        List<String> order = new ArrayList<>();
        tree.traverseBFS(n -> order.add(n.getName()));
        return String.join(",", order);
    }

}
