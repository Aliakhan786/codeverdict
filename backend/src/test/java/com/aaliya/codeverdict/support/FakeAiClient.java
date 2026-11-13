package com.aaliya.codeverdict.support;

import com.aaliya.codeverdict.ai.AiClient;
import com.aaliya.codeverdict.ai.AiResponse;
import com.aaliya.codeverdict.ai.dto.BrokenTest;
import com.aaliya.codeverdict.ai.dto.ChargeDraft;
import com.aaliya.codeverdict.ai.dto.ChargeForTest;
import com.aaliya.codeverdict.ai.dto.ChargesResponse;
import com.aaliya.codeverdict.ai.dto.FixDraft;
import com.aaliya.codeverdict.ai.dto.FixesResponse;
import com.aaliya.codeverdict.ai.dto.ProblemContext;
import com.aaliya.codeverdict.ai.dto.ProofTestDraft;
import com.aaliya.codeverdict.ai.dto.ProofTestsResponse;
import com.aaliya.codeverdict.ai.dto.QualityResponse;
import com.aaliya.codeverdict.ai.dto.ReferenceDraft;
import com.aaliya.codeverdict.ai.dto.TestInputDraft;
import com.aaliya.codeverdict.ai.dto.TestInputsResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * A scripted "AI" for the Maximum Subarray problem, so tests never call Gemini.
 * It deliberately files one TRUE charge, one FALSE charge and one charge with a
 * WRONG test, to exercise every branch of the double-check rule.
 */
public class FakeAiClient implements AiClient {

    public static final String SIGNATURE = "int maxSubArray(int[] nums)";

    public static final String BUGGY_USER_CODE = """
            class Solution {
                public int maxSubArray(int[] nums) {
                    int best = 0;
                    for (int i = 0; i < nums.length; i++) {
                        int sum = 0;
                        for (int j = i; j < nums.length; j++) {
                            sum += nums[j];
                            best = Math.max(best, sum);
                        }
                    }
                    return best;
                }
            }
            """;

    public static final String CORRECT_USER_CODE = """
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
            """;

    private static final String KADANE = """
            class Solution {
                public int maxSubArray(int[] nums) {
                    int best = nums[0], cur = 0;
                    for (int x : nums) { cur = Math.max(x, cur + x); best = Math.max(best, cur); }
                    return best;
                }
            }
            """;

    private static final String BRUTE = """
            class Solution {
                public int maxSubArray(int[] nums) {
                    int best = Integer.MIN_VALUE;
                    for (int i = 0; i < nums.length; i++) {
                        int s = 0;
                        for (int j = i; j < nums.length; j++) { s += nums[j]; best = Math.max(best, s); }
                    }
                    return best;
                }
            }
            """;

    private static final String HEADER = "import org.junit.jupiter.api.*;\nimport static org.junit.jupiter.api.Assertions.*;\n";

    @Override
    public AiResponse<ChargesResponse> findCharges(ProblemContext p, String code) {
        return r(new ChargesResponse(List.of(
                new ChargeDraft("BUG", "HIGH", 3, 3, "Wrong answer when all numbers are negative",
                        "best starts at 0, so [-3,-1,-2] returns 0 instead of -1", "WRONG_CONDITION"),
                new ChargeDraft("EDGE_CASE", "MEDIUM", 4, 4, "Crash on a single element",
                        "(the AI is wrong here)", "MISSING_EMPTY_CHECK"),
                new ChargeDraft("BUG", "LOW", 7, 7, "Sum is off by one",
                        "(this test has a wrong expected value)", "OFF_BY_ONE"),
                new ChargeDraft("PERFORMANCE", "MEDIUM", 4, 6, "Quadratic nested loops",
                        "Kadane's algorithm is O(n)", "INEFFICIENT_ALGORITHM"))));
    }

    @Override
    public AiResponse<ProofTestsResponse> writeProofTests(ProblemContext p, String code, List<ChargeForTest> charges) {
        List<ProofTestDraft> tests = new ArrayList<>();
        for (ChargeForTest c : charges) {
            String body = switch (c.title()) {
                case "Wrong answer when all numbers are negative" ->
                        "assertEquals(-1, new Solution().maxSubArray(new int[]{-3, -1, -2}));";
                case "Crash on a single element" -> "assertEquals(5, new Solution().maxSubArray(new int[]{5}));";
                default -> "assertEquals(7, new Solution().maxSubArray(new int[]{1, 2, 3}))"; // missing ';' -> repaired
            };
            tests.add(new ProofTestDraft(c.chargeId(),
                    HEADER + "class AnyName {\n    @Test\n    void proof() {\n        " + body + "\n    }\n}\n"));
        }
        return r(new ProofTestsResponse(tests));
    }

    @Override
    public AiResponse<ProofTestsResponse> repairProofTests(ProblemContext p, String code, List<BrokenTest> broken) {
        List<ProofTestDraft> tests = new ArrayList<>();
        for (BrokenTest b : broken) {
            tests.add(new ProofTestDraft(b.chargeId(), b.testClassCode().replace("{1, 2, 3}))", "{1, 2, 3}));")));
        }
        return r(new ProofTestsResponse(tests));
    }

    @Override
    public AiResponse<FixesResponse> writeFixes(ProblemContext p, String code, List<ChargeForTest> charges) {
        List<FixDraft> fixes = new ArrayList<>();
        for (ChargeForTest c : charges) {
            fixes.add(new FixDraft(c.chargeId(), code.replace("int best = 0;", "int best = Integer.MIN_VALUE;"),
                    "Start best at Integer.MIN_VALUE so all-negative arrays work."));
        }
        return r(new FixesResponse(fixes));
    }

    @Override
    public AiResponse<ReferenceDraft> writeReference(ProblemContext p, boolean retry) {
        return r(new ReferenceDraft(KADANE, BRUTE, "O(n)", "Kadane: keep the best sum of a subarray ending here."));
    }

    @Override
    public AiResponse<TestInputsResponse> writeTestInputs(ProblemContext p) {
        return r(new TestInputsResponse(List.of(
                new TestInputDraft("new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}", false, "sample"),
                new TestInputDraft("new int[]{-1}", true, "single negative"),
                new TestInputDraft("new int[]{-3, -1, -2}", true, "all negative"),
                new TestInputDraft("new int[]{5, 4, -1, 7, 8}", false, "mostly positive"),
                new TestInputDraft("new int[]{0}", true, "single zero"),
                new TestInputDraft("new int[]{}", true, "empty: invalid for this problem, must be dropped"),
                new TestInputDraft("new int[]{1, 2", false, "broken syntax, must be dropped")),
                "static Object[] generate(int n, Random r) { int[] a = new int[n]; "
                        + "for (int i = 0; i < n; i++) a[i] = r.nextInt(201) - 100; return new Object[]{a}; }"));
    }

    @Override
    public AiResponse<QualityResponse> rateQuality(ProblemContext p, String code) {
        return r(new QualityResponse(4, 4, 3, 5, "Clear names.", "Easy to read.", "Could return early.",
                "Nothing to improve.", "O(n^2)"));
    }

    @Override
    public String modelName() {
        return "fake-ai";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    private static <T> AiResponse<T> r(T value) {
        return new AiResponse<>(value, 100, 1, false);
    }
}
