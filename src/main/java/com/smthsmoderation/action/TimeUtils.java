package com.smthsmoderation.action;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and formats the short duration strings used in command templates
 * and the smart penalty multiplier ("30m", "2h", "1d", "1w", "3mo", "45s").
 */
public final class TimeUtils {

    private static final long MINUTES_PER_MONTH = 43_200; // 30-day month
    private static final long MINUTES_PER_WEEK = 10_080;
    private static final long MINUTES_PER_DAY = 1_440;
    private static final long MINUTES_PER_HOUR = 60;

    private static final Pattern DURATION = Pattern.compile("^(\\d+(?:\\.\\d+)?)(mo|[smhdw])?$");

    private TimeUtils() {
    }

    /** Returns 0 for null, blank, or unparsable input. */
    public static long parseMinutes(String input) {
        if (input == null || input.isBlank()) return 0;
        Matcher matcher = DURATION.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return 0;

        double value = Double.parseDouble(matcher.group(1));
        double minutes = switch (matcher.group(2) == null ? "m" : matcher.group(2)) {
            case "s" -> value / 60.0;
            case "h" -> value * MINUTES_PER_HOUR;
            case "d" -> value * MINUTES_PER_DAY;
            case "w" -> value * MINUTES_PER_WEEK;
            case "mo" -> value * MINUTES_PER_MONTH;
            default -> value;
        };
        return Math.round(minutes);
    }

    public static long multiplyMinutes(long baseMinutes, double multiplier) {
        return Math.round(baseMinutes * multiplier);
    }

    /** Compact display form, e.g. 1440 -> "1d", 90 -> "1h 30m". */
    public static String formatMinutes(long minutes) {
        if (minutes <= 0) return "0m";
        if (minutes % MINUTES_PER_MONTH == 0) return (minutes / MINUTES_PER_MONTH) + "mo";
        if (minutes >= MINUTES_PER_MONTH) return withRemainder(minutes, MINUTES_PER_MONTH, "mo");
        if (minutes % MINUTES_PER_WEEK == 0) return (minutes / MINUTES_PER_WEEK) + "w";
        if (minutes >= MINUTES_PER_WEEK) return withRemainder(minutes, MINUTES_PER_WEEK, "w");
        if (minutes % MINUTES_PER_DAY == 0) return (minutes / MINUTES_PER_DAY) + "d";
        if (minutes >= MINUTES_PER_DAY) return withRemainder(minutes, MINUTES_PER_DAY, "d");
        if (minutes % MINUTES_PER_HOUR == 0) return (minutes / MINUTES_PER_HOUR) + "h";
        if (minutes >= MINUTES_PER_HOUR) return withRemainder(minutes, MINUTES_PER_HOUR, "h");
        return minutes + "m";
    }

    private static String withRemainder(long minutes, long unit, String suffix) {
        long whole = minutes / unit;
        long remainder = minutes % unit;
        return remainder == 0 ? whole + suffix : whole + suffix + " " + formatMinutes(remainder);
    }
}
