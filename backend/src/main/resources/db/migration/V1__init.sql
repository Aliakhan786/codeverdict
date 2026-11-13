-- CodeVerdict schema. A user has problems; a problem has submissions (attempts);
-- each submission gets one review; a review has many charges, hidden tests and benchmark results.

CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    display_name   VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',      -- USER | DEMO
    theme          VARCHAR(10)  NOT NULL DEFAULT 'dark',      -- dark | light
    sound_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    daily_goal     INTEGER      NOT NULL DEFAULT 2,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE problems (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title            VARCHAR(200) NOT NULL,
    statement        TEXT         NOT NULL,
    topic            VARCHAR(30)  NOT NULL,
    difficulty       VARCHAR(10)  NOT NULL,                   -- EASY | MEDIUM | HARD
    method_signature VARCHAR(300) NOT NULL,                   -- e.g. int[] twoSum(int[] nums, int target)
    sample_input     TEXT,                                    -- Java argument list, e.g. new int[]{2,7,11,15}, 9
    sample_output    TEXT,                                    -- e.g. [0, 1]
    compare_mode     VARCHAR(20)  NOT NULL DEFAULT 'EXACT',   -- EXACT | UNORDERED | UNORDERED_DEEP
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE submissions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id     UUID        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    attempt_no     INTEGER     NOT NULL,
    code           TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL,                      -- QUEUED | RUNNING | DONE | FAILED
    current_stage  VARCHAR(30),
    failure_reason TEXT,
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_submission_attempt UNIQUE (problem_id, attempt_no)
);

CREATE TABLE reviews (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id        UUID        NOT NULL UNIQUE REFERENCES submissions (id) ON DELETE CASCADE,
    kind                 VARCHAR(20) NOT NULL,                -- FULL | COMPILE_ERROR
    verdict_score        INTEGER     NOT NULL,
    grade                VARCHAR(2)  NOT NULL,
    correctness_score    INTEGER,
    performance_score    INTEGER,
    edge_case_score      INTEGER,
    quality_score        INTEGER,
    summary              TEXT        NOT NULL,
    claimed_complexity   VARCHAR(40),
    measured_complexity  VARCHAR(40),
    complexity_measured  BOOLEAN     NOT NULL DEFAULT FALSE,
    optimal_complexity   VARCHAR(40),
    key_idea             TEXT,
    hidden_tests_passed  INTEGER     NOT NULL DEFAULT 0,
    hidden_tests_total   INTEGER     NOT NULL DEFAULT 0,
    ai_available         BOOLEAN     NOT NULL DEFAULT TRUE,
    reference_verified   BOOLEAN     NOT NULL DEFAULT FALSE,
    ai_model             VARCHAR(60),
    ai_tokens_used       INTEGER     NOT NULL DEFAULT 0,
    ai_calls             INTEGER     NOT NULL DEFAULT 0,
    duration_ms          BIGINT      NOT NULL DEFAULT 0,
    xp_earned            INTEGER     NOT NULL DEFAULT 0,
    new_level            VARCHAR(30),
    personal_best        BOOLEAN     NOT NULL DEFAULT FALSE,
    details              JSONB,                               -- compile errors, quality tips, warnings, ...
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE charges (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id          UUID         NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    position           INTEGER      NOT NULL,
    category           VARCHAR(20)  NOT NULL,                 -- BUG | EDGE_CASE | PERFORMANCE | STYLE
    severity           VARCHAR(10)  NOT NULL,                 -- HIGH | MEDIUM | LOW
    line_start         INTEGER      NOT NULL,
    line_end           INTEGER      NOT NULL,
    title              VARCHAR(300) NOT NULL,
    explanation        TEXT,
    status             VARCHAR(20)  NOT NULL,                 -- PROVEN | DISMISSED | ACQUITTED | ERROR | NOTED
    status_reason      TEXT,
    bad_evidence       BOOLEAN      NOT NULL DEFAULT FALSE,
    proof_test_code    TEXT,
    proof_output       TEXT,
    evidence_line      INTEGER,
    suggested_fix_code TEXT,
    fix_explanation    TEXT,
    fix_verified       BOOLEAN,
    mistake_tag        VARCHAR(40)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE hidden_tests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id       UUID        NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    position        INTEGER     NOT NULL,
    input           TEXT        NOT NULL,
    expected_output TEXT,
    actual_output   TEXT,
    passed          BOOLEAN     NOT NULL,
    is_edge_case    BOOLEAN     NOT NULL DEFAULT FALSE,
    description     VARCHAR(200),
    error           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE benchmarks (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id  UUID             NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    solution   VARCHAR(10)      NOT NULL,                     -- USER | OPTIMAL
    input_size INTEGER          NOT NULL,
    median_ms  DOUBLE PRECISION,
    runs       INTEGER          NOT NULL DEFAULT 0,
    status     VARCHAR(20)      NOT NULL,                     -- OK | TOO_SLOW | ERROR | SKIPPED
    error      TEXT,
    created_at TIMESTAMPTZ      NOT NULL DEFAULT now()
);

-- The trusted "case file" per problem: reference solutions, hidden-test inputs, input generator.
CREATE TABLE optimal_solutions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id        UUID        NOT NULL UNIQUE REFERENCES problems (id) ON DELETE CASCADE,
    code              TEXT        NOT NULL,
    brute_force_code  TEXT,
    complexity        VARCHAR(40),
    key_idea          TEXT,
    generator_code    TEXT,
    test_inputs       JSONB,
    verified          BOOLEAN     NOT NULL DEFAULT FALSE,
    verification_note TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_stats (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    total_xp         INTEGER     NOT NULL DEFAULT 0,
    level            VARCHAR(30) NOT NULL DEFAULT 'Intern',
    current_streak   INTEGER     NOT NULL DEFAULT 0,
    longest_streak   INTEGER     NOT NULL DEFAULT 0,
    last_active_date DATE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE xp_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    source        VARCHAR(20)  NOT NULL,                      -- REVIEW | FIX | BADGE | STREAK
    points        INTEGER      NOT NULL,
    submission_id UUID         REFERENCES submissions (id) ON DELETE SET NULL,
    description   VARCHAR(200),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE achievements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(40)  NOT NULL UNIQUE,
    name        VARCHAR(60)  NOT NULL,
    description VARCHAR(200) NOT NULL,
    icon        VARCHAR(40)  NOT NULL,
    xp_reward   INTEGER      NOT NULL DEFAULT 25,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_achievements (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    achievement_id UUID        NOT NULL REFERENCES achievements (id) ON DELETE CASCADE,
    submission_id  UUID        REFERENCES submissions (id) ON DELETE SET NULL,
    unlocked_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_achievement UNIQUE (user_id, achievement_id)
);

-- Indexes for the dashboard queries
CREATE INDEX idx_problems_user ON problems (user_id);
CREATE INDEX idx_submissions_user_created ON submissions (user_id, created_at);
CREATE INDEX idx_submissions_problem ON submissions (problem_id);
CREATE INDEX idx_reviews_submission ON reviews (submission_id);
CREATE INDEX idx_charges_review ON charges (review_id);
CREATE INDEX idx_charges_mistake_tag ON charges (mistake_tag);
CREATE INDEX idx_hidden_tests_review ON hidden_tests (review_id);
CREATE INDEX idx_benchmarks_review ON benchmarks (review_id);
CREATE INDEX idx_xp_events_user ON xp_events (user_id, created_at);
