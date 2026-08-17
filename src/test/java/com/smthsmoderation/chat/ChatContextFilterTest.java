package com.smthsmoderation.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatContextFilterTest {

    @Test
    void keepsChatShapedLinesAndHighlightsTarget() {
        List<String> filtered = ChatContextFilter.filter(List.of("Steve: hello there"), "Steve", 30);
        assertEquals(1, filtered.size());
        assertTrue(filtered.get(0).contains("[1;31mSteve[0m"));
    }

    @Test
    void dropsLinesWithoutChatShape() {
        List<String> filtered = ChatContextFilter.filter(List.of("just some random text"), "Steve", 30);
        assertTrue(filtered.isEmpty());
    }

    @Test
    void dropsAnticheatAndVersionNoise() {
        List<String> filtered = ChatContextFilter.filter(
                List.of("Server: AntiCheat -> Steve flagged", "Server: new version -> update available"),
                "Steve", 30);
        assertTrue(filtered.isEmpty());
    }

    @Test
    void dropsBoxDrawingAndArrowLines() {
        List<String> filtered = ChatContextFilter.filter(List.of("Steve » hi"), "Steve", 30);
        assertFalse(filtered.stream().anyMatch(l -> l.contains("»")));
    }

    @Test
    void stripsLeadingYiSyllableSymbol() {
        // U+A000 is inside the Yi-syllable block the filter strips as a leading evasion symbol.
        String withYiPrefix = "ꀀSteve: hello";
        List<String> filtered = ChatContextFilter.filter(List.of(withYiPrefix), "Steve", 30);
        assertEquals(1, filtered.size());
        assertFalse(filtered.get(0).startsWith("ꀀ"));
    }

    @Test
    void clampsToLastMaxMessages() {
        List<String> lines = List.of("A: 1", "B: 2", "C: 3", "D: 4");
        List<String> filtered = ChatContextFilter.filter(lines, "X", 2);
        assertEquals(2, filtered.size());
    }

    @Test
    void chunksWrapInAnsiCodeBlock() {
        List<String> chunks = ChatContextFilter.chunk(List.of("Steve: hi"));
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).startsWith("```ansi\n"));
        assertTrue(chunks.get(0).endsWith("\n```"));
    }

    @Test
    void emptyInputProducesNoChunks() {
        assertTrue(ChatContextFilter.chunk(List.of()).isEmpty());
    }
}
