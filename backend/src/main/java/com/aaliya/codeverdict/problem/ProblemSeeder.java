package com.aaliya.codeverdict.problem;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Adds the starter problems to a new account. */
@Component
public class ProblemSeeder {

    private final ProblemRepository problems;

    public ProblemSeeder(ProblemRepository problems) {
        this.problems = problems;
    }

    @Transactional
    public void seedStarterProblems(UUID userId) {
        if (problems.countByUserId(userId) > 0) {
            return;
        }
        for (StarterProblems.Starter s : StarterProblems.ALL) {
            Problem p = new Problem();
            p.setUserId(userId);
            p.setTitle(s.title());
            p.setStatement(s.statement());
            p.setTopic(s.topic());
            p.setDifficulty(s.difficulty());
            p.setMethodSignature(s.signature());
            p.setSampleInput(s.sampleInput());
            p.setSampleOutput(s.sampleOutput());
            p.setCompareMode(s.mode());
            problems.save(p);
        }
    }
}
