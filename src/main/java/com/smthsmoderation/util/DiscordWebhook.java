package com.smthsmoderation.util;

import com.smthsmoderation.config.ActionsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {

    private static final int MAX_CHAT_LENGTH = 1000;
    private static final int COLOR_RED = 16711680;
    private static final int COLOR_ORANGE = 16753920;
    private static final int COLOR_BLUE = 3447003;
    private static final int COLOR_GREEN = 65280;
    private static final int COLOR_DEFAULT = 16733525;

    private static int getActionColor(String actionType) {
        if (actionType == null) return COLOR_DEFAULT;
        return switch (actionType.toLowerCase().trim()) {
            case "ban" -> COLOR_RED;
            case "mute" -> COLOR_ORANGE;
            case "kick" -> COLOR_BLUE;
            case "warn" -> COLOR_GREEN;
            default -> COLOR_DEFAULT;
        };
    }

    public static void sendEmbed(String playerName, String actionType, String duration,
                                  String reason, String command, int color,
                                  List<String> chatHistory) {
        if (!ActionsManager.enableWebhook) return;
        String url = ActionsManager.webhookUrl;
        if (url == null || url.isBlank()) return;

        String moderator = "?";
        try {
            moderator = MinecraftClient.getInstance().getSession().getUsername();
        } catch (Exception ignored) {}

        int embedColor = getActionColor(actionType);
        String chatBlock = buildChatBlock(chatHistory);

        StringBuilder fields = new StringBuilder();
        fields.append("{\"name\":\"Moderator\",\"value\":\"").append(escapeJson(moderator)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Target Player\",\"value\":\"").append(escapeJson(playerName)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Action Type\",\"value\":\"").append(escapeJson(actionType)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Duration\",\"value\":\"").append(escapeJson(duration.isEmpty() ? "N/A" : duration)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Reason\",\"value\":\"").append(escapeJson(reason.isEmpty() ? "N/A" : reason)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Executed Command\",\"value\":\"`").append(escapeJson(command)).append("`\",\"inline\":false}");
        if (!chatBlock.isEmpty()) {
            fields.append(",{\"name\":\"Recent Chat Context\",\"value\":\"").append(escapeJson(chatBlock)).append("\",\"inline\":false}");
        }

        String json = "{"
            + "\"username\":\"SmthsModeration\","
            + "\"embeds\":[{"
            + "\"title\":\"\uD83D\uDD28 Punishment Executed\","
            + "\"color\":" + embedColor + ","
            + "\"fields\":[" + fields + "],"
            + "\"timestamp\":\"" + Instant.now() + "\""
            + "}]"
            + "}";

        CompletableFuture.runAsync(() -> {
            HttpsURLConnection conn = null;
            try {
                conn = (HttpsURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                if (code != 200 && code != 204) {
                    String body = readErrorStream(conn);
                    sendClientMessage("\u00a7c[SmthsModeration] Webhook failed: HTTP " + code + (body.isEmpty() ? "" : " - " + body));
                }
            } catch (Exception e) {
                sendClientMessage("\u00a7c[SmthsModeration] Webhook error: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String buildChatBlock(List<String> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) return "";
        int maxMessages = Math.max(1, Math.min(50, ActionsManager.webhookMessageCount));
        List<String> lastMessages = chatHistory.size() > maxMessages
            ? chatHistory.subList(chatHistory.size() - maxMessages, chatHistory.size())
            : chatHistory;
        StringBuilder raw = new StringBuilder();
        for (String msg : lastMessages) {
            raw.append(msg).append("\n");
        }
        String history = raw.toString();
        int maxHistoryLength = 950;
        if (history.length() > maxHistoryLength) {
            history = history.substring(0, maxHistoryLength) + "\n...[TRUNCATED]";
        }
        return "```text\n" + history + "\n```";
    }

    private static void sendClientMessage(String msg) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal(msg), false);
            }
        } catch (Exception ignored) {}
    }

    private static String readErrorStream(HttpsURLConnection conn) {
        try {
            var stream = conn.getErrorStream();
            if (stream == null) return "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            int maxLen = 200;
            while ((line = reader.readLine()) != null && sb.length() < maxLen) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
