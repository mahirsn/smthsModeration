package com.smthsmoderation.log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes one text file per executed punishment to {@code <gameDir>/smthsmoderations-logs/}.
 * The filename includes a nanosecond suffix (not just second-precision time)
 * so two punishments on the same player within the same second don't
 * collide and overwrite each other.
 */
public final class PunishmentLogger {

    private static final DateTimeFormatter FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PunishmentLogger() {
    }

    public static void write(Path logDir, String playerName, String actionType, String duration, String reason,
                              String command, List<String> chatBacklog) throws IOException {
        Files.createDirectories(logDir);

        LocalDateTime now = LocalDateTime.now();
        String fileName = FILENAME_TIMESTAMP.format(now) + "_" + playerName + "_" + (System.nanoTime() & 0xFFFFF) + ".txt";

        StringBuilder content = new StringBuilder();
        content.append("=== PUNISHMENT LOG ===\n");
        content.append("Target Player: ").append(playerName).append("\n");
        content.append("Action Type: ").append(actionType).append("\n");
        content.append("Duration: ").append(duration.isEmpty() ? "N/A" : duration).append("\n");
        content.append("Reason: ").append(reason.isEmpty() ? "N/A" : reason).append("\n");
        content.append("Executed Command: /").append(command).append("\n");
        content.append("Date & Time: ").append(DISPLAY_TIMESTAMP.format(now)).append("\n\n");

        content.append("=== RECENT CHAT HISTORY (Last ").append(chatBacklog.size()).append(" messages) ===\n");
        for (String line : chatBacklog) {
            content.append(line).append("\n");
        }

        Files.writeString(logDir.resolve(fileName), content.toString());
    }
}
