import java.util.*;

class Solution {
    public int[][] merge(int[][] intervals) {
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] in : intervals) {
            if (!merged.isEmpty() && merged.get(merged.size() - 1)[1] >= in[0]) {
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], in[1]);
            } else {
                merged.add(new int[]{in[0], in[1]});
            }
        }
        return merged.toArray(new int[0][]);
    }
}
