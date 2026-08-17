package com.smthsmoderation.action;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandTemplateTest {

    @Test
    void fillsPlayerAndVariables() {
        String result = CommandTemplate.fill("/ban %player% %duration% %reason%", "Steve",
                Map.of("duration", "1d", "reason", "hile"));
        assertEquals("/ban Steve 1d hile", result);
    }

    @Test
    void blanksUnfilledPlaceholders() {
        String result = CommandTemplate.fill("/mute %player% %duration% %reason%", "Steve", Map.of());
        assertEquals("/mute Steve", result);
    }

    @Test
    void previewShowsEllipsisForUnfilledValues() {
        String preview = CommandTemplate.preview("/ban %player% %duration%", "", Map.of("duration", ""));
        assertEquals("/ban ... ...", preview);
    }

    @Test
    void stripsLeadingSlashOnlyIfPresent() {
        assertEquals("ban Steve", CommandTemplate.stripLeadingSlash("/ban Steve"));
        assertEquals("ban Steve", CommandTemplate.stripLeadingSlash("ban Steve"));
    }
}
