# Interview guide

Know this file well. Interviewers spend under a minute on the README and then ask *how* and *why*.

## 30-second pitch

> "CodeVerdict is a code-review platform for DSA practice. Most AI reviewers guess, so I made one that proves.
> The AI only *suggests* bugs; for each one it writes a JUnit test, and my backend runs that test in a sandbox on my code
> **and** on a verified reference solution. A bug counts only if the test fails on my code and passes on the reference —
> so a badly written AI test can never convict correct code. It also times my code against the optimal solution and tracks
> my mistakes over time. It's Java 21 and Spring Boot, React and TypeScript, PostgreSQL, Docker and Gemini."

## 2-minute demo script

| Time | Show |
|---|---|
| 0:00–0:15 | Home: streak, level, today's challenge |
| 0:15–0:40 | Write a solution with an edge-case bug, Submit, live stages stream in |
| 0:40–1:10 | Verdict reveal: one GUILTY charge with the test and the real error; one DISMISSED charge ("the AI guessed wrong, we caught it") |
| 1:10–1:25 | Apply fix → ACQUITTED; speed chart vs optimal |
| 1:25–1:45 | Rematch → higher score, confetti / badge |
| 1:45–2:00 | Progress dashboard: weekly score, top mistakes |

## The life of one review (with file names)

1. `SubmissionController.submit` → `SubmissionService.create` saves a QUEUED row (rate limit, one active job per user) → returns **202**.
2. `ReviewJobService.start` puts the job on a bounded 2-thread pool (`AsyncConfig`).
3. The browser opens `GET /submissions/{id}/events`; `SubmissionEventBus` streams stages with **SSE** and replays past stages to late subscribers.
4. `ProofEngine.review` (no Spring inside — easy to test):
   - `CodeSafetyChecker` → `JavaSourceCompiler` (in-process javac). Errors stop here, no AI cost.
   - `CvCall` is compiled *with* the user's class: a compile error there means "wrong method signature".
   - In parallel: AI **charges** + AI **quality** (`aiAsync`), while the **reference** is prepared:
     optimal + brute force, verified on the sample and by **differential testing** on ~60 inputs; cached per problem (`optimal_solutions`).
   - AI writes one JUnit test per BUG/EDGE_CASE charge (one batched call). Each test is safety-checked and compiled alone; broken ones get **one** repair call.
   - JUnit console launcher runs all tests on the user classpath and on the reference classpath → `JUnitReportParser` → `VerdictClassifier` (**double-check rule**).
   - Hidden tests: AI inputs + seeded random inputs; expected = reference output (`CaseRunner`, `GenRunner`).
   - Fixes: compiled, proof test re-run, hidden tests re-run → ACQUITTED only if no regression.
   - `BenchRunner`: fixed seed, 3 warm-ups, median of 5 timed runs, 2 s limit per run → `PerformanceJudge`, `ComplexityEstimator`.
   - `ScoringCalculator`.
5. `ReviewPersistenceService.save` stores review, charges, tests, timings, case file **and** XP/badges (`GamificationService`) in **one transaction**.
6. `VERDICT_READY` is pushed; React fetches `GET /submissions/{id}` and plays the reveal.

## Questions you will get

**Why not just trust the AI?**
LLMs produce plausible but wrong review comments. A test is executable evidence. My evaluation measures exactly how many false alarms the proof step removes (docs/eval/results.md).

**What if the AI writes a wrong test?**
That's the double-check rule. Every test also runs on a verified reference. If it fails there too, the test is wrong, not the code — the charge is DISMISSED and counted as "bad evidence".

**How do you know the reference is right?**
The AI writes two independent solutions (optimal and brute force). I only trust the reference if it matches the sample output and agrees with the brute force on every one of ~60 inputs. If not, I retry once; if still unverified, the verdict shows a warning.

**How do you run untrusted code safely?**
Two layers. (1) A static check before anything runs: only `java.util`/`java.math` imports; no files, network, threads, reflection, `System.exit`, native methods; unicode escapes are rejected because javac decodes them before parsing and they can hide code inside a string. (2) A Docker container per run: `--network none --memory 256m --memory-swap 256m --cpus 1 --pids-limit 64 --read-only --user 65534 --cap-drop ALL`, killed on timeout and removed after the run. Plus hard timeouts on compile, each test and the whole job, and stdout capped at 64 KB.
*Infinite loop?* JUnit's per-test timeout in a separate thread + process timeout. *Fork bomb?* `--pids-limit`. *1 GB allocation?* heap cap → `OutOfMemoryError`, reported as evidence.

**Why asynchronous? Why SSE instead of WebSockets or polling?**
A review takes seconds to a minute; holding an HTTP request open that long is fragile. Updates only go server → browser, so SSE is enough: plain HTTP, works through proxies, the browser reconnects automatically, and EventSource sends the auth cookie. Polling wastes requests; WebSockets add a bidirectional protocol I don't need.

**How do you measure performance fairly?**
Same machine, same seed for inputs, input generation outside the timed region, 3 warm-up runs so the JIT compiles hot code, median of 5 (robust to GC pauses), and a 0.5 ms noise floor so tiny timings can't fake a 3× difference. Complexity comes from the slope of log(time) vs log(n); I only call it "measured" if at least two sizes take ≥ 0.25 ms and the slopes agree. O(n) vs O(n log n) differ by ~0.1 in slope, so I'm honest that measurement alone can't separate them.

**How is the score calculated? What is measured vs AI-rated?**
Correctness 40 (hidden tests), performance 30 (benchmark), edge cases 10 (proven charges) are measured = 80%. Only code quality (20) is AI opinion and the UI labels it. If a part can't be computed (AI down), the score is scaled over the parts that were, instead of punishing the user.

**How do you handle Gemini failures and rate limits? What does a review cost?**
JSON mode with a response schema; invalid JSON → one retry; HTTP 429/5xx → exponential backoff (2 s, 4 s); results cached by hash of task + code; reference and inputs cached per problem in the DB. ~5 calls per review; independent calls run in parallel; tokens and calls are stored per review. If Gemini is down: compile + hidden tests + benchmark from the cache, with a "partial verdict" banner (`UnavailableAiClient`, `markAiDown`).

**How would you scale to 10,000 users?**
Move the in-memory queue to Redis/RabbitMQ; separate stateless runner workers (autoscaled) with a pool of pre-warmed containers (or gVisor/Firecracker for stronger isolation); SSE events via Redis pub/sub so any API node can stream; read replicas for dashboards; per-user quotas. The engine is already decoupled from Spring, so it can move into the worker as is.

**Which design patterns?**
Strategy (`CodeRunner`: local/docker, `AiClient`: Gemini/unavailable), Decorator (`CachingAiClient`), Pipeline (stages in `ProofEngine`), Repository (Spring Data), DTO records, global exception handler.

**Interesting SQL?**
Personal best per problem: `ROW_NUMBER() OVER (PARTITION BY problem_id ORDER BY verdict_score DESC)`; weekly averages with `date_trunc('week', created_at AT TIME ZONE :tz)`; top mistakes with `GROUP BY mistake_tag` + `array_agg`; matching timings with `LEFT JOIN LATERAL`. Indexes on `submissions(user_id, created_at)`, `charges(review_id)`, `charges(mistake_tag)`.

**Security of the web app?**
BCrypt passwords; JWT (HS256, secret from env) in an **httpOnly, SameSite=Lax** cookie — JavaScript can't read it (XSS) and it isn't sent on cross-site POSTs (CSRF); CORS limited to the frontend origin; same error for unknown user and wrong password; Gemini key only on the server; input size limits; CSV export neutralises spreadsheet formulas.

**What would you do differently / limitations?**
Java only; benchmark noise on shared hardware; in-memory job queue; AI-written tests can be weak (the double-check rule protects correctness but can't make a weak test find more bugs); problems with many valid answers need the right comparison mode.

**Did you use AI to build it?**
Answer honestly: yes, as a tool — then show you understand it by walking through any file above and changing something live (e.g. a scoring weight in `ScoringCalculator` or a rule in `CodeSafetyChecker`, with its unit test).

## Resume entry (fill the brackets with YOUR measured numbers only)

**CodeVerdict — AI Code Review & Performance Platform** | Java, Spring Boot, React, TypeScript, PostgreSQL, Docker, Gemini API

- Built a code review platform that proves each AI-reported bug by generating and running a JUnit test in a Docker sandbox, and verifies suggested fixes by re-running the test.
- Introduced a double-check rule (test must fail on user code and pass on a verified reference), cutting false alarms from [X] to [Y] across a [30]-snippet evaluation set.
- Built a React dashboard with live progress (SSE), benchmark-based complexity checks and progress analytics; used it daily across [N] practice problems, raising my average score from [A] to [B].
