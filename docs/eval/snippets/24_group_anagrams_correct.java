import java.util.*;

class Solution {
    public List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String word : strs) {
            int[] counts = new int[26];
            for (char c : word.toCharArray()) {
                counts[c - 'a']++;
            }
            groups.computeIfAbsent(Arrays.toString(counts), k -> new ArrayList<>()).add(word);
        }
        return new ArrayList<>(groups.values());
    }
}
