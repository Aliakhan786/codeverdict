package com.aaliya.codeverdict.problem;

import com.aaliya.codeverdict.proof.MethodSignature;

/** Builds the editor's starting code from a method signature. */
public final class StarterCode {

    private StarterCode() {
    }

    public static String of(String methodSignature) {
        MethodSignature sig;
        try {
            sig = MethodSignature.parse(methodSignature);
        } catch (IllegalArgumentException e) {
            return "import java.util.*;\n\nclass Solution {\n}\n";
        }
        String ret = switch (sig.returnType()) {
            case "void" -> "";
            case "int", "long", "short", "byte" -> "        return 0;\n";
            case "double", "float" -> "        return 0.0;\n";
            case "boolean" -> "        return false;\n";
            case "char" -> "        return ' ';\n";
            default -> "        return null;\n";
        };
        return "import java.util.*;\n\nclass Solution {\n    public " + sig + " {\n        // Write your solution here\n"
                + ret + "    }\n}\n";
    }
}
