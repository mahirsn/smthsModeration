package com.smthsmoderation.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationActionTest {

    @Test
    void detectsReasonAndDurationFromTemplate() {
        ModerationAction ban = new ModerationAction("ban", "/ban %player% %duration% %reason%", 0xFFFF5555, "");
        assertTrue(ban.requiresReason);
        assertTrue(ban.requiresDuration);
    }

    @Test
    void noPlaceholdersMeansNoRequirement() {
        ModerationAction tp = new ModerationAction("tp", "/tp %player%", 0xFF555555, "");
        assertFalse(tp.requiresReason);
        assertFalse(tp.requiresDuration);
    }

    @Test
    void zeroColorFallsBackToDefaultGray() {
        ModerationAction action = new ModerationAction("x", "/x %player%", 0, "");
        assertEquals(0xFFAAAAAA, action.getColor());
    }

    @Test
    void hoverColorIsLighterThanBase() {
        ModerationAction action = new ModerationAction("x", "/x %player%", 0xFF804020, "");
        int hover = action.getHoverColor();
        assertTrue(((hover >> 16) & 0xFF) > ((0xFF804020 >> 16) & 0xFF));
    }
}
