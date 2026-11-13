package com.aaliya.codeverdict.proof;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed form of a problem's method signature, e.g.
 * {@code int[] twoSum(int[] nums, int target)} or
 * {@code List<List<Integer>> threeSum(int[] nums)}.
 */
public record MethodSignature(String returnType, String name, List<Param> params) {

    public record Param(String type, String name) {
    }

    private static final Pattern HEAD = Pattern.compile(
            "^\\s*(?:public\\s+)?(?:static\\s+)?(.+?)\\s+([A-Za-z_$][\\w$]*)\\s*\\((.*)\\)\\s*;?\\s*$", Pattern.DOTALL);

    public static MethodSignature parse(String signature) {
        if (signature == null) {
            throw new IllegalArgumentException("Method signature is required");
        }
        Matcher m = HEAD.matcher(signature.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Method signature must look like: int[] twoSum(int[] nums, int target)");
        }
        String ret = normalize(m.group(1));
        String name = m.group(2);
        List<Param> params = new ArrayList<>();
        String inside = m.group(3).trim();
        if (!inside.isEmpty()) {
            for (String part : splitTopLevel(inside)) {
                String p = normalize(part);
                int cut = p.lastIndexOf(' ');
                if (cut <= 0) {
                    throw new IllegalArgumentException("Parameter needs a type and a name: '" + part.trim() + "'");
                }
                params.add(new Param(p.substring(0, cut).trim(), p.substring(cut + 1).trim()));
            }
        }
        return new MethodSignature(ret, name, List.copyOf(params));
    }

    public boolean isVoid() {
        return "void".equals(returnType);
    }

    /** Java expression that casts an Object to the given type ((int) o works through unboxing). */
    public static String castExpr(String type, String objectExpr) {
        return "((" + type + ") " + objectExpr + ")";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(returnType).append(' ').append(name).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params.get(i).type()).append(' ').append(params.get(i).name());
        }
        return sb.append(')').toString();
    }

    private static String normalize(String s) {
        // "int [ ] a" -> "int[] a", collapse spaces, keep generics tidy
        return s.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*\\[\\s*]", "[]")
                .replaceAll("\\s*<\\s*", "<")
                .replaceAll("\\s*>", ">")
                .replaceAll("\\s*,\\s*(?=[^<>]*>)", ",")
                .replaceAll("^final ", "");
    }

    /** Splits "Map<String, Integer> m, int k" on commas that are not inside generics. */
    static List<String> splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
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
