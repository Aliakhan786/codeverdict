package com.aaliya.codeverdict.proof;

import com.aaliya.codeverdict.proof.OutputComparator.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputComparatorTest {

    @Test
    void ignoresWhitespaceAndQuotes() {
        assertTrue(OutputComparator.same("[0, 1]", "[0,1]", Mode.EXACT));
        assertTrue(OutputComparator.same("\"abc\"", "abc", Mode.EXACT));
    }

    @Test
    void exactModeCaresAboutOrder() {
        assertFalse(OutputComparator.same("[1, 0]", "[0, 1]", Mode.EXACT));
    }

    @Test
    void unorderedSortsTopLevelOnly() {
        assertTrue(OutputComparator.same("[[1, 2], [3, 4]]", "[[3, 4], [1, 2]]", Mode.UNORDERED));
        assertFalse(OutputComparator.same("[[1, 2]]", "[[2, 1]]", Mode.UNORDERED));
    }

    @Test
    void unorderedDeepSortsEveryLevel() {
        assertTrue(OutputComparator.same("[[bat], [nat, tan], [ate, eat, tea]]",
                "[[eat, tea, ate], [tan, nat], [bat]]", Mode.UNORDERED_DEEP));
    }

    @Test
    void nullNeverMatches() {
        assertFalse(OutputComparator.same(null, "1", Mode.EXACT));
    }
}
