package com.aaliya.codeverdict.proof;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodSignatureTest {

    @Test
    void parsesSimpleSignature() {
        MethodSignature s = MethodSignature.parse("int[] twoSum(int[] nums, int target)");
        assertEquals("int[]", s.returnType());
        assertEquals("twoSum", s.name());
        assertEquals(2, s.params().size());
        assertEquals("int[]", s.params().get(0).type());
        assertEquals("target", s.params().get(1).name());
    }

    @Test
    void parsesGenericsWithCommas() {
        MethodSignature s = MethodSignature.parse("public List<List<Integer>> f(Map<String, Integer> m, int k)");
        assertEquals("List<List<Integer>>", s.returnType());
        assertEquals("Map<String,Integer>", s.params().get(0).type());
        assertEquals("k", s.params().get(1).name());
    }

    @Test
    void parsesVoidAndNoArgs() {
        MethodSignature s = MethodSignature.parse("void reset()");
        assertTrue(s.isVoid());
        assertEquals(0, s.params().size());
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> MethodSignature.parse("hello world"));
        assertThrows(IllegalArgumentException.class, () -> MethodSignature.parse("int f(int)"));
    }
}
