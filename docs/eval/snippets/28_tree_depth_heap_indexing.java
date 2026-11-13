class Solution {
    public int maxDepth(Integer[] tree) {
        return depth(tree, 0);
    }

    private int depth(Integer[] tree, int i) {
        if (i >= tree.length || tree[i] == null) {
            return 0;
        }
        return 1 + Math.max(depth(tree, 2 * i + 1), depth(tree, 2 * i + 2));
    }
}
