// TypeScript mirrors of the backend JSON (see backend/**/*Dtos.java).

export type Topic =
  | "ARRAYS" | "STRINGS" | "HASHMAP" | "TWO_POINTERS" | "SLIDING_WINDOW"
  | "RECURSION" | "DP" | "TREES" | "GRAPHS" | "SORTING";
export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type CompareMode = "EXACT" | "UNORDERED" | "UNORDERED_DEEP";
export type ChargeStatus = "PROVEN" | "DISMISSED" | "ACQUITTED" | "ERROR" | "NOTED";
export type ChargeCategory = "BUG" | "EDGE_CASE" | "PERFORMANCE" | "STYLE";
export type Severity = "HIGH" | "MEDIUM" | "LOW";
export type SubmissionStatus = "QUEUED" | "RUNNING" | "DONE" | "FAILED";
export type Stage =
  | "QUEUED" | "COMPILING" | "READING_CODE" | "GATHERING_EVIDENCE" | "RUNNING_TESTS"
  | "HIDDEN_TESTS" | "VERIFYING_FIXES" | "BENCHMARKING" | "SCORING" | "VERDICT_READY" | "FAILED";

export interface ApiErrorBody { error: string; message: string; details?: unknown[] }

export interface Stats {
  totalXp: number; level: string; levelIndex: number; nextLevel: string | null;
  xpIntoLevel: number; xpForNextLevel: number; currentStreak: number; longestStreak: number;
}

export interface Me {
  id: string; username: string; email: string; displayName: string; role: "USER" | "DEMO";
  theme: "dark" | "light"; soundEnabled: boolean; dailyGoal: number; readOnly: boolean; stats: Stats;
}

export interface AuthResponse { token: string; user: Me }
export interface AuthConfig { registrationEnabled: boolean; demoEnabled: boolean; demoUsername?: string; demoPassword?: string }

export interface Problem {
  id: string; title: string; statement: string; topic: Topic; topicLabel: string; difficulty: Difficulty;
  methodSignature: string; sampleInput?: string; sampleOutput?: string; compareMode: CompareMode;
  starterCode: string; attempts: number; bestScore?: number; bestGrade?: string; lastAttemptAt?: string; createdAt: string;
}

export interface ProblemRequest {
  title: string; statement: string; topic: Topic; difficulty: Difficulty; methodSignature: string;
  sampleInput?: string; sampleOutput?: string; compareMode: CompareMode;
}

export interface CompileError { line: number; column: number; message: string }

export interface RunResponse {
  compiled: boolean; compileErrors: CompileError[]; output?: string; expected?: string;
  passed?: boolean | null; error?: string; stdout?: string; timeMs: number;
}

export interface OptimalSolution { code: string; complexity?: string; keyIdea?: string; verified: boolean; verificationNote?: string }

export interface SubmitResponse { submissionId: string; status: SubmissionStatus; attemptNo: number }

export interface ProgressEvent { stage: Stage; label: string; detail?: string; progress: number; terminal: boolean; at: string }

export interface Charge {
  id: string; category: ChargeCategory; severity: Severity; lineStart: number; lineEnd: number; title: string;
  explanation?: string; status: ChargeStatus; statusReason?: string; badEvidence: boolean; proofTestCode?: string;
  proofOutput?: string; evidenceLine?: number; suggestedFixCode?: string; fixExplanation?: string;
  fixVerified?: boolean; mistakeTag: string; mistakeLabel: string;
}

export interface HiddenTest {
  input: string; expected?: string; actual?: string; passed: boolean; edgeCase: boolean; description?: string; error?: string;
}

export interface Point { n: number; ms?: number; status: string }

export interface Performance {
  claimedComplexity?: string; measuredComplexity?: string; complexityMeasured: boolean; slope?: number;
  optimalComplexity?: string; keyIdea?: string; comparedN?: number; ratio?: number; user: Point[]; optimal: Point[];
}

export interface Quality {
  naming: number; readability: number; structure: number; duplication: number;
  namingTip: string; readabilityTip: string; structureTip: string; duplicationTip: string; estimatedComplexity?: string;
}

export interface Badge {
  code: string; name: string; description: string; icon: string; xpReward: number; unlocked: boolean; unlockedAt?: string;
}

export interface Review {
  kind: "FULL" | "COMPILE_ERROR"; verdictScore: number; grade: string; summary: string;
  scores: { correctness?: number; edgeCases?: number; performance?: number; quality?: number; partial: boolean };
  charges: Charge[];
  hiddenTests: { passed: number; total: number; cases: HiddenTest[] };
  performance: Performance; quality?: Quality; compileErrors: CompileError[]; warnings: string[];
  aiAvailable: boolean; referenceVerified: boolean; referenceNote?: string; aiModel?: string;
  aiTokensUsed: number; aiCalls: number; durationMs: number; xpEarned: number; newLevel?: string;
  personalBest: boolean; newBadges: Badge[];
}

export interface Verdict {
  submissionId: string; problemId: string; problemTitle: string; topic: Topic; topicLabel: string;
  difficulty: Difficulty; methodSignature: string; attemptNo: number; code: string; status: SubmissionStatus;
  stage?: Stage; failureReason?: string; createdAt: string; completedAt?: string;
  previousAttempt?: { submissionId: string; attemptNo: number; verdictScore: number };
  review?: Review;
}

export interface HistoryRow {
  submissionId: string; problemId: string; problemTitle: string; topic: Topic; difficulty: Difficulty;
  attemptNo: number; status: SubmissionStatus; verdictScore?: number; grade?: string; bugsProven: number;
  durationMs?: number; createdAt: string;
}

export interface Page<T> { items: T[]; page: number; size: number; total: number; totalPages: number }

export interface AttemptSide {
  submissionId: string; attemptNo: number; code: string; verdictScore?: number; grade?: string;
  correctness?: number; edgeCases?: number; performance?: number; quality?: number; bugsProven: number; createdAt: string;
}

export interface CompareResponse { problemId: string; problemTitle: string; a: AttemptSide; b: AttemptSide; scoreDelta?: number }

export interface Summary {
  displayName: string; level: string; levelIndex: number; nextLevel?: string; totalXp: number; xpIntoLevel: number;
  xpForNextLevel: number; currentStreak: number; longestStreak: number; activeToday: boolean; todayCount: number;
  dailyGoal: number; week: { reviews: number; avgScore?: number; bugsProven: number };
  todaysChallenge?: { problemId: string; title: string; topic: Topic; topicLabel: string; difficulty: Difficulty; bestScore?: number; reason: string };
  continueCases: { problemId: string; title: string; topic: Topic; topicLabel: string; bestScore?: number; bestGrade?: string; attempts: number; lastAttemptAt: string }[];
}

export interface ProgressData {
  weeklyScores: { week: string; avgScore: number; reviews: number }[];
  topicStrength: { topic: Topic; label: string; avgScore: number; reviews: number }[];
  topMistakes: { tag: string; label: string; count: number; submissionIds: string[] }[];
  heatmap: { date: string; count: number }[];
  counters: { totalCases: number; bugsProven: number; fixesVerified: number; matchedOptimal: number; aiFalseAlarmsCaught: number };
}

export interface PersonalBest {
  problemId: string; title: string; topic: Topic; topicLabel: string; difficulty: Difficulty; bestScore: number;
  grade: string; attempts: number; bestSubmissionId: string; inputSize?: number; userMs?: number; optimalMs?: number;
  ratio?: number; allHiddenPassed: boolean; rating: "GREEN" | "YELLOW" | "RED";
}

export interface SystemStatus { aiConfigured: boolean; aiModel?: string; runner: string; maxReviewsPerHour: number; timezone: string }
