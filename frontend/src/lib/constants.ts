import type { ChargeStatus, CompareMode, Difficulty, Stage, Topic } from "@/types/api";

export const TOPICS: { value: Topic; label: string }[] = [
  { value: "ARRAYS", label: "Arrays" },
  { value: "STRINGS", label: "Strings" },
  { value: "HASHMAP", label: "HashMap" },
  { value: "TWO_POINTERS", label: "Two Pointers" },
  { value: "SLIDING_WINDOW", label: "Sliding Window" },
  { value: "RECURSION", label: "Recursion" },
  { value: "DP", label: "DP" },
  { value: "TREES", label: "Trees" },
  { value: "GRAPHS", label: "Graphs" },
  { value: "SORTING", label: "Sorting" },
];

export const topicLabel = (t?: string) => TOPICS.find((x) => x.value === t)?.label ?? t ?? "";

export const DIFFICULTIES: Difficulty[] = ["EASY", "MEDIUM", "HARD"];

export const COMPARE_MODES: { value: CompareMode; label: string; hint: string }[] = [
  { value: "EXACT", label: "Exact", hint: "Output must match exactly (whitespace ignored)" },
  { value: "UNORDERED", label: "Any order", hint: "Top-level list items may come in any order" },
  { value: "UNORDERED_DEEP", label: "Any order (nested)", hint: "Order ignored at every level, e.g. Group Anagrams" },
];

/** Shown in this order on the Verdict page while a review runs. */
export const STAGES: { stage: Stage; label: string; courtroom: string }[] = [
  { stage: "COMPILING", label: "Compiling", courtroom: "Calling the court to order" },
  { stage: "READING_CODE", label: "Reading the code", courtroom: "The prosecution reads the case file" },
  { stage: "GATHERING_EVIDENCE", label: "Gathering evidence", courtroom: "Writing a proof test for every charge" },
  { stage: "RUNNING_TESTS", label: "Running the evidence", courtroom: "Evidence on your code and on the reference" },
  { stage: "HIDDEN_TESTS", label: "Hidden tests", courtroom: "Questioning the witnesses" },
  { stage: "VERIFYING_FIXES", label: "Verifying fixes", courtroom: "The defence presents its fixes" },
  { stage: "BENCHMARKING", label: "Benchmarking", courtroom: "Timing you against the optimal solution" },
  { stage: "SCORING", label: "Scoring", courtroom: "The jury deliberates" },
];

export const STATUS_META: Record<ChargeStatus, { label: string; court: string; className: string }> = {
  PROVEN: { label: "Guilty", court: "Proven by test", className: "text-guilty border-guilty/40 bg-guilty/10" },
  ACQUITTED: { label: "Acquitted", court: "Fix verified", className: "text-acquitted border-acquitted/40 bg-acquitted/10" },
  DISMISSED: { label: "Dismissed", court: "Not proven", className: "text-dismissed border-dismissed/40 bg-dismissed/10" },
  ERROR: { label: "No evidence", court: "Could not test", className: "text-dismissed border-dismissed/30 bg-dismissed/5" },
  NOTED: { label: "Noted", court: "Style advice", className: "text-info border-info/40 bg-info/10" },
};

export const LEVELS = ["Intern", "Junior Dev", "Developer", "Senior Dev", "Staff Engineer", "Principal"];

export const SAMPLE_INPUT_HELP =
  "Java arguments, exactly as inside the call: new int[]{2, 7, 11, 15}, 9   or   \"abcabcbb\"   or   new int[][]{{1, 3}, {2, 6}}";
