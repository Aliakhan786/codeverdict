package com.aaliya.codeverdict.problem;

import com.aaliya.codeverdict.proof.OutputComparator.Mode;

import java.util.List;

/** Classic practice problems added to every new account so the app is useful on day one. */
public final class StarterProblems {

    private StarterProblems() {
    }

    public record Starter(String title, Topic topic, Difficulty difficulty, String signature, String statement,
                          String sampleInput, String sampleOutput, Mode mode) {
    }

    public static final List<Starter> ALL = List.of(
            new Starter("Two Sum", Topic.HASHMAP, Difficulty.EASY,
                    "int[] twoSum(int[] nums, int target)",
                    """
                    Given an array of integers nums and an integer target, return the indices of the two numbers \
                    that add up to target, as [i, j] with i < j.

                    Exactly one valid answer exists, and you may not use the same element twice.

                    Constraints: 2 <= nums.length <= 10^5; -10^9 <= nums[i], target <= 10^9.""",
                    "new int[]{2, 7, 11, 15}, 9", "[0, 1]", Mode.EXACT),
            new Starter("Valid Parentheses", Topic.STRINGS, Difficulty.EASY,
                    "boolean isValid(String s)",
                    """
                    Given a string s containing only the characters '(', ')', '{', '}', '[' and ']', return true if \
                    every opening bracket is closed by the same type of bracket in the correct order.

                    Constraints: 1 <= s.length() <= 10^5.""",
                    "\"()[]{}\"", "true", Mode.EXACT),
            new Starter("Binary Search", Topic.ARRAYS, Difficulty.EASY,
                    "int search(int[] nums, int target)",
                    """
                    Given an array of distinct integers nums sorted in ascending order and an integer target, return \
                    the index of target, or -1 if it is not in the array. Aim for O(log n).

                    Constraints: 1 <= nums.length <= 10^5; -10^9 <= nums[i], target <= 10^9.""",
                    "new int[]{-1, 0, 3, 5, 9, 12}, 9", "4", Mode.EXACT),
            new Starter("Maximum Subarray", Topic.DP, Difficulty.MEDIUM,
                    "int maxSubArray(int[] nums)",
                    """
                    Given an integer array nums, find the contiguous subarray (containing at least one number) with the \
                    largest sum and return that sum.

                    Constraints: 1 <= nums.length <= 10^5; -10^4 <= nums[i] <= 10^4.""",
                    "new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}", "6", Mode.EXACT),
            new Starter("Longest Substring Without Repeating Characters", Topic.SLIDING_WINDOW, Difficulty.MEDIUM,
                    "int lengthOfLongestSubstring(String s)",
                    """
                    Given a string s, return the length of the longest substring that has no repeated characters.

                    Constraints: 0 <= s.length() <= 5 * 10^4; s consists of printable ASCII characters.""",
                    "\"abcabcbb\"", "3", Mode.EXACT),
            new Starter("Container With Most Water", Topic.TWO_POINTERS, Difficulty.MEDIUM,
                    "int maxArea(int[] height)",
                    """
                    You are given an array height of n non-negative integers. Choose two lines i < j; together with the \
                    x-axis they form a container holding min(height[i], height[j]) * (j - i) water. Return the maximum \
                    amount of water a container can hold.

                    Constraints: 2 <= height.length <= 10^5; 0 <= height[i] <= 10^4.""",
                    "new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7}", "49", Mode.EXACT),
            new Starter("Merge Intervals", Topic.SORTING, Difficulty.MEDIUM,
                    "int[][] merge(int[][] intervals)",
                    """
                    Given an array of intervals where intervals[i] = [start, end], merge all overlapping intervals \
                    (intervals that touch, like [1,4] and [4,5], also merge) and return the merged intervals sorted \
                    by start.

                    Constraints: 1 <= intervals.length <= 10^5; 0 <= start <= end <= 10^9.""",
                    "new int[][]{{1, 3}, {2, 6}, {8, 10}, {15, 18}}", "[[1, 6], [8, 10], [15, 18]]", Mode.EXACT),
            new Starter("Group Anagrams", Topic.HASHMAP, Difficulty.MEDIUM,
                    "List<List<String>> groupAnagrams(String[] strs)",
                    """
                    Given an array of strings strs, group the anagrams together. Groups and the words inside each group \
                    may be returned in any order.

                    Constraints: 1 <= strs.length <= 10^4; 0 <= strs[i].length() <= 100; lowercase English letters.""",
                    "new String[]{\"eat\", \"tea\", \"tan\", \"ate\", \"nat\", \"bat\"}",
                    "[[bat], [nat, tan], [ate, eat, tea]]", Mode.UNORDERED_DEEP),
            new Starter("Number of Islands", Topic.GRAPHS, Difficulty.MEDIUM,
                    "int numIslands(char[][] grid)",
                    """
                    Given an m x n grid of '1' (land) and '0' (water), return the number of islands. An island is \
                    land connected horizontally or vertically and surrounded by water.

                    Constraints: 1 <= m, n <= 300.""",
                    "new char[][]{{'1', '1', '0', '0'}, {'1', '0', '0', '1'}, {'0', '0', '1', '1'}}", "2", Mode.EXACT),
            new Starter("Maximum Depth of Binary Tree", Topic.TREES, Difficulty.EASY,
                    "int maxDepth(Integer[] tree)",
                    """
                    A binary tree is given in level order as an Integer array, where null means "no node" \
                    (LeetCode style): [3, 9, 20, null, null, 15, 7] is root 3 with children 9 and 20, and 20 has \
                    children 15 and 7. Return the maximum depth (number of nodes on the longest root-to-leaf path). \
                    An empty array is an empty tree with depth 0.

                    Constraints: 0 <= number of nodes <= 10^4.""",
                    "new Integer[]{3, 9, 20, null, null, 15, 7}", "3", Mode.EXACT)
    );
}
