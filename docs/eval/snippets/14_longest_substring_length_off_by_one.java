import java.util.*;

class Solution {
    public int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> last = new HashMap<>();
        int start = 0, best = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (last.containsKey(c)) {
                start = Math.max(start, last.get(c) + 1);
            }
            last.put(c, i);
            best = Math.max(best, i - start);
        }
        return best;
    }
}
