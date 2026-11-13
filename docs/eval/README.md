# Evaluation set

30 Java solutions to the 10 starter problems:

- **20 snippets with exactly one known bug** (wrong condition, off-by-one, missing empty check, infinite loop, wrong return, …)
- **10 fully correct snippets**

`answers.csv` holds the ground truth: `snippet, problem, has_bug, true_bug_line, bug_type`.

Every snippet was checked before it was added: each buggy snippet fails at least one hand-written test with a known
expected answer, and each correct snippet passes all of them.

Run the evaluation (real Gemini, ~30–60 min on the free tier):

```bash
cd backend
GEMINI_API_KEY=... mvn test -Dtest=EvaluationTest -Deval=true -Deval.runs=3
```

It writes `results.md` here. Only BUG and EDGE_CASE charges are counted; a charge is correct when its line range is
within 2 lines of the true bug line.
