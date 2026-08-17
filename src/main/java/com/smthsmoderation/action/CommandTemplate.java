package com.smthsmoderation.action;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fills a {@code commandTemplate} ("/ban %player% %duration% %reason%") with
 * the target player name and variable values. Any {@code %placeholder%} left
 * over after substitution is blanked out rather than sent to the server
 * verbatim — this matches the mod's existing behavior, so a command whose
 * optional variable was left empty (e.g. %duration%) is not itself an error,
 * but the caller is responsible for deciding whether that's acceptable.
 */
public final class CommandTemplate {

    private static final Pattern LEFTOVER_PLACEHOLDER = Pattern.compile("%[^%\\s]+%");

    private CommandTemplate() {
    }

    public static String fill(String template, String playerName, Map<String, String> variableValues) {
        String result = template.replace("%player%", playerName == null ? "" : playerName);
        for (Map.Entry<String, String> entry : variableValues.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue() == null ? "" : entry.getValue());
        }
        return LEFTOVER_PLACEHOLDER.matcher(result).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    /** Live preview variant: unfilled placeholders render as "..." instead of being blanked. */
    public static String preview(String template, String playerName, Map<String, String> variableValues) {
        String result = template.replace("%player%", playerName == null || playerName.isBlank() ? "..." : playerName);
        for (Map.Entry<String, String> entry : variableValues.entrySet()) {
            String value = entry.getValue();
            result = result.replace("%" + entry.getKey() + "%", value == null || value.isBlank() ? "..." : value);
        }
        return result;
    }

    /** Strips the leading '/' before sending a filled command to chat, if present. */
    public static String stripLeadingSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }
}
