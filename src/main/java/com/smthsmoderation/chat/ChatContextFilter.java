package com.smthsmoderation.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns raw chat backlog lines into the "Recent Chat Context" ANSI-coded
 * chunks sent to Discord: drops anticheat/version-notification noise and
 * non-chat-shaped lines, undoes a few common small-caps/box-drawing evasion
 * tricks, and highlights the punished player's name. Pure text in, text
 * out — no Minecraft or network types — so it's unit-testable on its own.
 */
public final class ChatContextFilter {

    private static final int MAX_CHUNK_LENGTH = 900;
    private static final int MAX_CHUNKS = 4;

    private static final String[] FILTER_KEYWORDS = {
            "anticheat", "antiexploit", "antieploit", "lpx",
            "grimac", "matrix", "vulcan", "exploit", "cheat", "ncp", "packet",
            "version", "update", "release", "outdated", "download",
            "yeni sürüm", "yeni bir sürüm",
            "web chat:", "available at"
    };

    private ChatContextFilter() {
    }

    /** Filtered, target-highlighted lines, clamped to the last {@code maxMessages}. */
    public static List<String> filter(List<String> lines, String targetName, int maxMessages) {
        if (lines == null || lines.isEmpty()) return List.of();

        List<String> clean = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("»")) continue;
            if (containsBoxDrawing(line)) continue;

            String stripped = containsYiSymbol(line) ? stripLeadingSymbol(line) : line;
            String normalized = normalizeSmallCaps(stripped);
            if (containsFilterKeyword(normalized)) continue;

            String lower = normalized.toLowerCase(Locale.ROOT);
            boolean unnamedSpyMention = lower.contains("casus")
                    && targetName != null && !normalized.contains(targetName);
            if (unnamedSpyMention) continue;

            boolean looksLikeChat = normalized.contains(":") || normalized.contains("->");
            if (!looksLikeChat) continue;

            clean.add(highlightTarget(normalized, targetName));
        }

        int limit = Math.max(1, Math.min(50, maxMessages));
        return clean.size() > limit ? clean.subList(clean.size() - limit, clean.size()) : clean;
    }

    /** Filtered lines packed into <=4 ANSI-code-block chunks of <=900 chars each. */
    public static List<String> chunk(List<String> filteredLines) {
        if (filteredLines.isEmpty()) return List.of();

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : filteredLines) {
            boolean wouldOverflow = current.length() + line.length() + 1 > MAX_CHUNK_LENGTH && !current.isEmpty();
            if (wouldOverflow) {
                chunks.add(current.toString());
                if (chunks.size() >= MAX_CHUNKS) return wrapAnsi(chunks);
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append("\n");
            current.append(line);
        }
        if (!current.isEmpty() && chunks.size() < MAX_CHUNKS) {
            chunks.add(current.toString());
        }
        return wrapAnsi(chunks);
    }

    private static List<String> wrapAnsi(List<String> chunks) {
        return chunks.stream().map(c -> "```ansi\n" + c + "\n```").toList();
    }

    private static boolean containsBoxDrawing(String line) {
        return line.chars().anyMatch(c -> c >= 0x2500 && c <= 0x257F);
    }

    private static boolean containsYiSymbol(String line) {
        return line.chars().anyMatch(c -> c >= 0xA000 && c <= 0xAFFF);
    }

    private static String stripLeadingSymbol(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c >= 0xA000 && c <= 0xAFFF) continue;
            return c == ' ' ? line.substring(i + 1) : line.substring(i);
        }
        return line;
    }

    private static boolean containsFilterKeyword(String normalized) {
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String keyword : FILTER_KEYWORDS) {
            if (lower.contains(keyword)) return true;
        }
        return false;
    }

    private static String highlightTarget(String line, String targetName) {
        if (targetName == null || targetName.isEmpty()) return line;
        return line.replace(targetName, "[1;31m" + targetName + "[0m");
    }

    /** Undoes common unicode "small caps" evasion of filter keywords, e.g. ᴀɴᴛɪᴄʜᴇᴀᴛ -> anticheat-shaped text. */
    private static String normalizeSmallCaps(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(smallCapsToAscii(c));
        }
        return out.toString();
    }

    private static char smallCapsToAscii(char c) {
        if (c >= 'ᴀ' && c <= 'ᴥ') return (char) (c - 'ᴀ' + 'A');
        if (c >= 'ᴬ' && c <= 'ᴯ') return (char) (c - 'ᴬ' + 'B');
        if (c == 'ᴰ') return 'D';
        if (c == 'ᴱ') return 'E';
        if (c >= 'ᴳ' && c <= 'ᴴ') return (char) (c - 'ᴳ' + 'G');
        if (c == 'ᴹ') return 'M';
        if (c == 'ᴽ') return 'O';
        if (c >= 'ᴾ' && c <= 'ᴿ') return (char) (c - 'ᴾ' + 'P');
        if (c == 'ᵀ') return 'R';
        if (c == 'ᵂ') return 'W';
        if (c == 'ᵃ') return 'a';
        if (c >= 'ᵇ' && c <= 'ᵉ') return (char) (c - 'ᵇ' + 'b');
        if (c == 'ᵊ') return 'e';
        if (c == 'ᵍ') return 'k';
        if (c == 'ᵏ') return 'o';
        if (c == 'ᵐ') return 'm';
        if (c == 'ᵒ') return 'p';
        if (c == 'ᵗ') return 't';
        if (c == 'ᵘ') return 'u';
        if (c == 'ᵛ') return 'v';
        if (c == 'ᶜ') return 'c';
        if (c == 'ᶟ') return 'f';
        if (c == 'ᶠ') return 'f';
        if (c == 'ₐ') return 'a';
        if (c == 'ₑ') return 'e';
        if (c == 'ₒ') return 'o';
        if (c == 'ₕ') return 'h';
        if (c == 'ₖ') return 'k';
        if (c == 'ₗ') return 'l';
        if (c == 'ₘ') return 'm';
        if (c == 'ₙ') return 'n';
        if (c == 'ₚ') return 'p';
        if (c == 'ₛ') return 's';
        if (c == 'ₜ') return 't';
        return c;
    }
}
