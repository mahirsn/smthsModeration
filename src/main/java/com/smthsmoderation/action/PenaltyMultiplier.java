package com.smthsmoderation.action;

/**
 * Smart penalty multiplier: escalates a base duration based on a player's
 * counted prior infractions for one action type.
 */
public final class PenaltyMultiplier {

    private PenaltyMultiplier() {
    }

    /**
     * @return 0 if {@code infractionCount <= 0} (no multiplier applies),
     *         otherwise {@code clamp(1.0 + infractionCount * step, 1.0, max)}.
     */
    public static double compute(int infractionCount, double step, double max) {
        if (infractionCount <= 0) return 0;
        double multiplier = 1.0 + infractionCount * step;
        return Math.min(Math.max(multiplier, 1.0), max);
    }

    /** Applies {@link #compute} to a base duration string, e.g. "30m" -> minutes. */
    public static long applyToBaseDuration(String baseDuration, int infractionCount, double step, double max) {
        double multiplier = compute(infractionCount, step, max);
        if (multiplier <= 0) return 0;
        return TimeUtils.multiplyMinutes(TimeUtils.parseMinutes(baseDuration), multiplier);
    }
}
