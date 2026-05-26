package com.smthsmoderation.util;

public class TimeUtils {

    // --- Time conversion constants (in minutes) ---
    public static final long MINUTES_PER_MONTH = 43200;
    public static final long MINUTES_PER_WEEK  = 10080;
    public static final long MINUTES_PER_DAY   = 1440;
    public static final long MINUTES_PER_HOUR  = 60;

    /** Parse a time string like "30m", "2h", "1d", "1mo" into minutes. */
    public static long parseMinutes(String input) {
        if (input == null || input.isBlank()) return 0;
        String s = input.trim().toLowerCase();
        double val;
        try {
            if (s.endsWith("mo")) {
                val = Double.parseDouble(s.replace("mo", "").trim()) * MINUTES_PER_MONTH; // 30-day month
            } else if (s.endsWith("w")) {
                val = Double.parseDouble(s.replace("w", "").trim()) * MINUTES_PER_WEEK; // 7-day week
            } else if (s.endsWith("d")) {
                val = Double.parseDouble(s.replace("d", "").trim()) * MINUTES_PER_DAY;
            } else if (s.endsWith("h")) {
                val = Double.parseDouble(s.replace("h", "").trim()) * MINUTES_PER_HOUR;
            } else if (s.endsWith("m")) {
                val = Double.parseDouble(s.replace("m", "").trim());
            } else if (s.endsWith("s")) {
                val = Double.parseDouble(s.replace("s", "").trim()) / 60.0;
            } else {
                val = Double.parseDouble(s);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return Math.round(val);
    }

    /** Multiply a base-minutes value by a double factor, return new minutes. */
    public static long multiplyMinutes(long baseMinutes, double multiplier) {
        return Math.round(baseMinutes * multiplier);
    }

    /** Format minutes back to the cleanest compact string (e.g. 1440 -> "1d"). */
    public static String formatMinutes(long minutes) {
        if (minutes <= 0) return "0m";
        if (minutes >= MINUTES_PER_MONTH && minutes % MINUTES_PER_MONTH == 0) return (minutes / 43200) + "mo";
        if (minutes >= 43200) return formatMinutesWithRemainder(minutes, 43200, "mo");
        if (minutes >= MINUTES_PER_WEEK && minutes % MINUTES_PER_WEEK == 0) return (minutes / 10080) + "w";
        if (minutes >= 10080) return formatMinutesWithRemainder(minutes, 10080, "w");
        if (minutes >= MINUTES_PER_DAY && minutes % MINUTES_PER_DAY == 0) return (minutes / 1440) + "d";
        if (minutes >= 1440) return formatMinutesWithRemainder(minutes, 1440, "d");
        if (minutes >= MINUTES_PER_HOUR && minutes % MINUTES_PER_HOUR == 0) return (minutes / 60) + "h";
        if (minutes >= 60) return formatMinutesWithRemainder(minutes, 60, "h");
        return minutes + "m";
    }

    private static String formatMinutesWithRemainder(long minutes, long unit, String suffix) {
        long whole = minutes / unit;
        long rem = minutes % unit;
        if (rem == 0) return whole + suffix;
        String remainder = formatMinutes(rem);
        return whole + suffix + " " + remainder;
    }
}
