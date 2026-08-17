package com.smthsmoderation.webhook;

import com.smthsmoderation.chat.ChatContextFilter;
import com.smthsmoderation.config.ActionsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Posts a punishment summary embed to a Discord webhook. Opt-in only: if
 * {@code webhookUrl} is blank, this does nothing. There is no built-in
 * fallback webhook — a prior version of this mod shipped one hardcoded and
 * gated to two specific server domains, which is a credential that doesn't
 * belong in a public jar.
 */
public final class DiscordWebhook {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static final int COLOR_RED = 0xFF5555;
    private static final int COLOR_ORANGE = 0xFFA500;
    private static final int COLOR_BLUE = 0x3498DB;
    private static final int COLOR_GREEN = 0x00FF00;
    private static final int COLOR_DEFAULT = 0xFF3355;

    private DiscordWebhook() {
    }

    public static void sendEmbed(String playerName, String actionType, String duration, String reason,
                                  String command, List<String> chatHistory) {
        if (!ActionsManager.config.enableWebhook) return;
        String url = ActionsManager.config.webhookUrl;
        if (url == null || url.isBlank()) return;

        String moderator = currentUsername();
        List<String> filtered = ChatContextFilter.filter(chatHistory, playerName, ActionsManager.config.webhookMessageCount);
        List<String> chunks = ChatContextFilter.chunk(filtered);

        String json = buildPayload(moderator, playerName, actionType, duration, reason, command, chunks);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    int status = response.statusCode();
                    if (status != 200 && status != 204) {
                        notifyPlayer("§c[SmthsModeration] Webhook failed: HTTP " + status);
                    }
                })
                .exceptionally(error -> {
                    notifyPlayer("§c[SmthsModeration] Webhook error: " + error.getMessage());
                    return null;
                });
    }

    private static String buildPayload(String moderator, String playerName, String actionType, String duration,
                                        String reason, String command, List<String> chatChunks) {
        StringBuilder fields = new StringBuilder();
        appendField(fields, "Moderator", moderator, true, false);
        appendField(fields, "Target Player", playerName, true, false);
        appendField(fields, "Action Type", actionType, true, false);
        appendField(fields, "Duration", duration.isEmpty() ? "N/A" : duration, true, false);
        appendField(fields, "Reason", reason.isEmpty() ? "N/A" : reason, true, false);
        appendField(fields, "Executed Command", "`" + command + "`", false, false);
        for (int i = 0; i < chatChunks.size(); i++) {
            String label = chatChunks.size() == 1
                    ? "Recent Chat Context"
                    : "Recent Chat Context (Part " + (i + 1) + "/" + chatChunks.size() + ")";
            appendField(fields, label, chatChunks.get(i), false, true);
        }

        return "{"
                + "\"username\":\"SmthsModeration\","
                + "\"embeds\":[{"
                + "\"title\":\"🔨 Punishment Executed\","
                + "\"color\":" + actionColor(actionType) + ","
                + "\"fields\":[" + fields + "],"
                + "\"timestamp\":\"" + Instant.now() + "\""
                + "}]"
                + "}";
    }

    private static void appendField(StringBuilder fields, String name, String value, boolean inline, boolean isFirstOfContinuation) {
        if (!fields.isEmpty()) fields.append(",");
        fields.append("{\"name\":\"").append(escapeJson(name)).append("\",")
                .append("\"value\":\"").append(escapeJson(value)).append("\",")
                .append("\"inline\":").append(inline).append("}");
    }

    private static int actionColor(String actionType) {
        if (actionType == null) return COLOR_DEFAULT;
        return switch (actionType.toLowerCase(Locale.ROOT).trim()) {
            case "ban" -> COLOR_RED;
            case "mute" -> COLOR_ORANGE;
            case "kick" -> COLOR_BLUE;
            case "warn" -> COLOR_GREEN;
            default -> COLOR_DEFAULT;
        };
    }

    private static String currentUsername() {
        try {
            return Minecraft.getInstance().getUser().getName();
        } catch (Exception e) {
            return "?";
        }
    }

    private static void notifyPlayer(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}
