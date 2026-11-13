package com.aaliya.codeverdict.review;

import com.aaliya.codeverdict.auth.AuthUser;
import com.aaliya.codeverdict.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/charges")
@Tag(name = "Charges")
public class ChargeController {

    public record ApplyFixResponse(UUID chargeId, UUID problemId, String code, boolean fixVerified) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ChargeController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/{id}/apply-fix")
    @Operation(summary = "Returns the code with the suggested fix applied, ready to load into the editor")
    public ApplyFixResponse applyFix(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        List<ApplyFixResponse> rows = jdbc.query("""
                SELECT c.id, s.problem_id, c.suggested_fix_code, c.fix_verified
                FROM charges c JOIN reviews r ON r.id = c.review_id JOIN submissions s ON s.id = r.submission_id
                WHERE c.id = :cid AND s.user_id = :uid
                """, new MapSqlParameterSource().addValue("cid", id).addValue("uid", me.id()),
                (rs, i) -> new ApplyFixResponse(rs.getObject("id", UUID.class), rs.getObject("problem_id", UUID.class),
                        rs.getString("suggested_fix_code"), rs.getBoolean("fix_verified")));
        if (rows.isEmpty()) {
            throw ApiException.notFound("Charge");
        }
        if (rows.get(0).code() == null) {
            throw ApiException.badRequest("This charge has no suggested fix");
        }
        return rows.get(0);
    }
}
