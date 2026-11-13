package com.aaliya.codeverdict.proof;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compares two serialized outputs. Whitespace and quotes are ignored.
 * Some problems accept answers in any order, so a problem can choose:
 * EXACT, UNORDERED (top-level elements in any order) or UNORDERED_DEEP (every level).
 */
public final class OutputComparator {

    public enum Mode { EXACT, UNORDERED, UNORDERED_DEEP }

    private OutputComparator() {
    }

    public static boolean same(String expected, String actual, Mode mode) {
        if (expected == null || actual == null) {
            return false;
        }
        String a = normalize(expected);
        String b = normalize(actual);
        if (a.equals(b)) {
            return true;
        }
        if (mode == Mode.EXACT) {
            return false;
        }
        return canonical(a, mode == Mode.UNORDERED_DEEP, true).equals(canonical(b, mode == Mode.UNORDERED_DEEP, true));
    }

    public static String normalize(String s) {
        return s.replaceAll("\\s+", "").replace("\"", "");
    }

    /** Sorts list elements so that order does not matter. */
    static String canonical(String s, boolean deep, boolean top) {
        if (s.length() < 2 || s.charAt(0) != '[' || s.charAt(s.length() - 1) != ']') {
            return s;
        }
        List<String> items = splitTopLevel(s.substring(1, s.length() - 1));
        List<String> canon = new ArrayList<>();
        for (String it : items) {
            canon.add(deep ? canonical(it, true, false) : it);
        }
        if (top || deep) {
            Collections.sort(canon);
        }
        return "[" + String.join(",", canon) + "]";
    }

    private static List<String> splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        if (s.isEmpty()) {
            return parts;
        }
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[' || c == '{') {
                depth++;
            } else if (c == ']' || c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }
}
