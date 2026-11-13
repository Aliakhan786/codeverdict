package com.aaliya.codeverdict.runner;

import com.aaliya.codeverdict.runner.CodeSafetyChecker.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeSafetyCheckerTest {

    private final CodeSafetyChecker checker = new CodeSafetyChecker();

    @Test
    void normalSolutionPasses() {
        String code = """
                import java.util.*;
                import java.util.stream.*;
                class Solution {
                    // reading a File? no, just a comment mentioning Runtime and Thread
                    public int f(int[] a) { String s = "System.exit(0)"; return Arrays.stream(a).sum(); }
                }
                """;
        assertTrue(checker.check(code, Profile.SOLUTION).isEmpty());
    }

    @Test
    void blocksDangerousCalls() {
        assertFalse(checker.check("class Solution { void f() { System.exit(1); } }", Profile.SOLUTION).isEmpty());
        assertFalse(checker.check("class Solution { void f() throws Exception { Runtime.getRuntime().exec(\"ls\"); } }", Profile.SOLUTION).isEmpty());
        assertFalse(checker.check("class Solution { void f() { new Thread(() -> {}).start(); } }", Profile.SOLUTION).isEmpty());
        assertFalse(checker.check("import java.io.File;\nclass Solution {}", Profile.SOLUTION).isEmpty());
        assertFalse(checker.check("class Solution { Object f() throws Exception { return Class.forName(\"x\"); } }", Profile.SOLUTION).isEmpty());
        assertFalse(checker.check("class Solution { void f() { java.net.Socket s = null; } }", Profile.SOLUTION).isEmpty());
    }

    @Test
    void reportsTheRightLine() {
        var v = checker.check("class Solution {\n  void f() {\n    System.exit(0);\n  }\n}", Profile.SOLUTION);
        assertEquals(3, v.get(0).line());
    }

    @Test
    void blocksUnicodeEscapeTricks() {
        // " is a quote: javac decodes it before parsing, so it can hide code inside a "string"
        assertFalse(checker.check("class Solution { String s = \"\\u0022; System.exit(0); //\"; }", Profile.SOLUTION).isEmpty());
    }

    @Test
    void junitImportsOnlyAllowedForTests() {
        String test = "import org.junit.jupiter.api.*;\nimport static org.junit.jupiter.api.Assertions.*;\nclass ProofTest_c1 {}";
        assertTrue(checker.check(test, Profile.TEST).isEmpty());
        assertFalse(checker.check(test, Profile.SOLUTION).isEmpty());
    }
}
