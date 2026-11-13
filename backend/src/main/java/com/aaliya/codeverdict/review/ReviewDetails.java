package com.aaliya.codeverdict.review;

import com.aaliya.codeverdict.ai.dto.QualityResponse;

import java.util.List;

/** Extra review data stored as JSONB (variable shape, never filtered on in SQL). */
public record ReviewDetails(
        List<CompileErrorItem> compileErrors,
        QualityResponse quality,
        List<String> warnings,
        String referenceNote,
        Double complexitySlope,
        Integer comparedN,
        Double userMs,
        Double optimalMs,
        Double performanceRatio,
        boolean scorePartial) {

    public record CompileErrorItem(long line, long column, String message) {
    }
}
