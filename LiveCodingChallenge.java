package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class TreeNode {
    final int value;
    final TreeNode left;
    final TreeNode right;
    TreeNode(int value, TreeNode left, TreeNode right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }

    TreeNode(int value) {
        this.value = value;
        this.left= null;
        this.right = null;
    }

    TreeNode(int value, TreeNode left) {
        this.value = value;
        this.left = left;
        this.right = null;
    }


}

public class Main {
    void main() {

        // node 7
        TreeNode seven = new TreeNode(7);

        // node 2
        TreeNode two = new TreeNode(2);

        // node 5
        TreeNode five = new TreeNode(5);

        // node 1
        TreeNode one = new TreeNode(1);

        // node thirteen
        TreeNode thirteen = new TreeNode(13);


        // node eleven
        TreeNode eleven = new TreeNode(11, seven, two);

        //node four
        TreeNode four = new TreeNode(4, five, one);

        // node eighth
        TreeNode eigth = new TreeNode(8, thirteen, four);

        // node 4 level one
        TreeNode fourLevelOne = new TreeNode(4, eleven);

        // node root
        TreeNode root = new TreeNode(5,fourLevelOne, eigth);

        List<List<Integer>> paths = this.findPaths(root, 22);

        // Print
        System.out.println("[");
        for (int i = 0; i < paths.size(); i++) {
            System.out.print(" " + paths.get(i));
            if (i < paths.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
        System.out.println("]");


    }

    public List<List<Integer>> findPaths(TreeNode root, long targetSum){

        List<List<Integer>> listOfPaths = new ArrayList<List<Integer>>();

        if (root == null) return listOfPaths;

        Queue<PathElement> queue = new LinkedList<>();

        List<Integer> initialPath = new ArrayList<>();
        initialPath.add(root.value);
        queue.add(new PathElement(root, root.value, initialPath));

        while (!queue.isEmpty()) {
            PathElement current = queue.poll();
            TreeNode node = current.node;

            if (node.left == null && node.right == null) {
                if (current.currentSum == targetSum) {
                    listOfPaths.add(current.path);
                }
            }

            if (node.left != null) {
                List<Integer> leftPath = new ArrayList<>(current.path);
                leftPath.add(node.left.value);
                queue.add(new PathElement(node.left, current.currentSum + node.left.value, leftPath));
            }

            if (node.right != null) {
                List<Integer> rightPath = new ArrayList<>(current.path);
                rightPath.add(node.right.value);
                queue.add(new PathElement(node.right, current.currentSum + node.right.value, rightPath));
            }
        }

        return listOfPaths;
    }

    private static class PathElement {
        final TreeNode node;
        final long currentSum;
        final List<Integer> path;

        PathElement(TreeNode node, long currentSum, List<Integer> path) {
            this.node = node;
            this.currentSum = currentSum;
            this.path = path;
        }
    }
}
