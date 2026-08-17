package com.smthsmoderation.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PenaltyMultiplierTest {

    @Test
    void zeroOrNegativeInfractionsMeansNoMultiplier() {
        assertEquals(0, PenaltyMultiplier.compute(0, 0.2, 3.0));
        assertEquals(0, PenaltyMultiplier.compute(-1, 0.2, 3.0));
    }

    @Test
    void scalesLinearlyWithStep() {
        assertEquals(1.4, PenaltyMultiplier.compute(2, 0.2, 3.0), 1e-9);
    }

    @Test
    void clampsAtMax() {
        assertEquals(3.0, PenaltyMultiplier.compute(50, 0.2, 3.0), 1e-9);
    }

    @Test
    void appliesToBaseDuration() {
        // base 30m, 2 infractions, step 0.2 -> multiplier 1.4 -> 42m
        assertEquals(42, PenaltyMultiplier.applyToBaseDuration("30m", 2, 0.2, 3.0));
        assertEquals(0, PenaltyMultiplier.applyToBaseDuration("30m", 0, 0.2, 3.0));
    }
}
