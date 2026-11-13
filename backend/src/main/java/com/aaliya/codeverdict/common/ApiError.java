package com.aaliya.codeverdict.common;

import java.util.List;

/** Error body used by every endpoint: { "error": "COMPILE_ERROR", "message": "...", "details": [...] } */
public record ApiError(String error, String message, List<?> details) {
}
