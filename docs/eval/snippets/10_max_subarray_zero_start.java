class Solution {
    public int maxSubArray(int[] nums) {
        int best = 0;
        int current = 0;
        for (int x : nums) {
            current = Math.max(x, current + x);
            best = Math.max(best, current);
        }
        return best;
    }
}
