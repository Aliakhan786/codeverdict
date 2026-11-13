package com.aaliya.codeverdict.problem;

import com.aaliya.codeverdict.auth.AuthUser;
import com.aaliya.codeverdict.problem.ProblemDtos.OptimalResponse;
import com.aaliya.codeverdict.problem.ProblemDtos.ProblemRequest;
import com.aaliya.codeverdict.problem.ProblemDtos.ProblemResponse;
import com.aaliya.codeverdict.problem.ProblemDtos.RunRequest;
import com.aaliya.codeverdict.problem.ProblemDtos.RunResponse;
import com.aaliya.codeverdict.problem.ProblemDtos.TopicOption;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/problems")
@Tag(name = "Problems")
public class ProblemController {

    private final ProblemService service;

    public ProblemController(ProblemService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List problems (optional filters: topic, difficulty)")
    public List<ProblemResponse> list(@AuthenticationPrincipal AuthUser me,
                                      @RequestParam(required = false) Topic topic,
                                      @RequestParam(required = false) Difficulty difficulty) {
        return service.list(me.id(), topic, difficulty);
    }

    @GetMapping("/topics")
    @Operation(summary = "All topics with display labels")
    public List<TopicOption> topics() {
        return Arrays.stream(Topic.values()).map(t -> new TopicOption(t, t.label())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a problem")
    public ProblemResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProblemRequest req) {
        return service.create(me.id(), req);
    }

    @GetMapping("/{id}")
    public ProblemResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.get(me.id(), id);
    }

    @PutMapping("/{id}")
    public ProblemResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                  @Valid @RequestBody ProblemRequest req) {
        return service.update(me.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.delete(me.id(), id);
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "Compile and run the code on the sample input only (quick check, no AI)")
    public RunResponse run(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                           @Valid @RequestBody RunRequest req) {
        return service.run(me.id(), id, req.code());
    }

    @GetMapping("/{id}/optimal")
    @Operation(summary = "Optimal solution (only after at least one attempt)")
    public OptimalResponse optimal(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.optimal(me.id(), id);
    }
}
