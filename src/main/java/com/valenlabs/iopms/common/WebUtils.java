package com.valenlabs.iopms.common;

import java.time.YearMonth;

/** Shared web helpers. */
public final class WebUtils {

    private WebUtils() {
    }

    /** Parses ?month=YYYY-MM or falls back to the current month. */
    public static YearMonth month(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new ApiException(400, "month must be in YYYY-MM format");
        }
    }

    /** Minimal CSV escaping. */
    public static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
