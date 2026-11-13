class Solution {
    public int maxSubArray(int[] nums) {
        int best = nums[0];
        int current = 0;
        for (int value : nums) {
            current = Math.max(value, current + value);
            best = Math.max(best, current);
        }
        return best;
    }
}
