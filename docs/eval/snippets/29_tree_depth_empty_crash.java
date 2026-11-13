import java.util.*;

class Solution {
    public int maxDepth(Integer[] tree) {
        if (tree[0] == null) {
            return 0;
        }
        int depth = 0;
        int next = 1;
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(0);
        while (!queue.isEmpty()) {
            depth++;
            int size = queue.size();
            for (int k = 0; k < size; k++) {
                queue.poll();
                for (int child = 0; child < 2; child++) {
                    if (next < tree.length) {
                        if (tree[next] != null) {
                            queue.add(next);
                        }
                        next++;
                    }
                }
            }
        }
        return depth;
    }
}
