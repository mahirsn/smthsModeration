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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

        String targetUrl = ActionsManager.webhookUrl;
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            boolean isAuthorizedServer = false;
            if (client.getCurrentServerEntry() != null) {
                String ip = client.getCurrentServerEntry().address.toLowerCase();
                if (ip.endsWith("blocksmiths.net") || ip.endsWith("blocksmp.net")) {
                    isAuthorizedServer = true;
                }
            }
            if (!isAuthorizedServer) return;
            String encodedWebhook = "aHR0cHM6Ly9kaXNjb3JkLmNvbS9hcGkvd2ViaG9va3MvMTUxMTc1MTE2MTM3NDQ0NTY1OC93VENpX0NvLVl1MUUtSjlDXzhic0lLRVhUSzdJN1h4eUFKNDgwNUU3VUhiRTFHTlZiVUljTUhmN2JBQzluNlVad3RweA==";
            targetUrl = new String(java.util.Base64.getDecoder().decode(encodedWebhook));
        }

        String moderator = "?";
        try {
            moderator = MinecraftClient.getInstance().getSession().getUsername();
        } catch (Exception ignored) {}

        int embedColor = getActionColor(actionType);
        List<String> chatChunks = buildChatChunks(chatHistory, playerName);

        StringBuilder fields = new StringBuilder();
        fields.append("{\"name\":\"Moderator\",\"value\":\"").append(escapeJson(moderator)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Target Player\",\"value\":\"").append(escapeJson(playerName)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Action Type\",\"value\":\"").append(escapeJson(actionType)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Duration\",\"value\":\"").append(escapeJson(duration.isEmpty() ? "N/A" : duration)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Reason\",\"value\":\"").append(escapeJson(reason.isEmpty() ? "N/A" : reason)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"Executed Command\",\"value\":\"`").append(escapeJson(command)).append("`\",\"inline\":false}");
        for (int i = 0; i < chatChunks.size(); i++) {
            String label = chatChunks.size() == 1
                ? "Recent Chat Context"
                : "Recent Chat Context (Part " + (i + 1) + "/" + chatChunks.size() + ")";
            fields.append(",{\"name\":\"").append(escapeJson(label)).append("\",\"value\":\"").append(escapeJson(chatChunks.get(i))).append("\",\"inline\":false}");
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

        final String finalUrl = targetUrl;
        CompletableFuture.runAsync(() -> {
            HttpsURLConnection conn = null;
            try {
                conn = (HttpsURLConnection) new URL(finalUrl).openConnection();
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

    private static final int MAX_CHUNK_LENGTH = 900;
    private static final int MAX_CHAT_CHUNKS = 4;

    private static final String[] FILTER_KEYWORDS = {
        "anticheat", "antiexploit", "antieploit", "lpx",
        "grimac", "matrix", "vulcan", "exploit", "cheat", "ncp", "packet"
    };

    private static boolean hasBoxDrawing(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c >= 0x2500 && c <= 0x257F) return true;
        }
        return false;
    }

    private static boolean hasYiSymbol(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c >= 0xA000 && c <= 0xAFFF) return true;
        }
        return false;
    }

    private static String normalizeSmallCaps(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u1D00' && c <= '\u1D25') sb.append((char) (c - '\u1D00' + 'A'));
            else if (c >= '\u1D2C' && c <= '\u1D2F') sb.append((char) (c - '\u1D2C' + 'B'));
            else if (c == '\u1D30') sb.append('D');
            else if (c == '\u1D31') sb.append('E');
            else if (c >= '\u1D33' && c <= '\u1D34') sb.append((char) (c - '\u1D33' + 'G'));
            else if (c == '\u1D39') sb.append('M');
            else if (c == '\u1D3D') sb.append('O');
            else if (c >= '\u1D3E' && c <= '\u1D3F') sb.append((char) (c - '\u1D3E' + 'P'));
            else if (c == '\u1D40') sb.append('R');
            else if (c >= '\u1D42' && c <= '\u1D4A') {
                if (c == '\u1D42') sb.append('W');
                else if (c == '\u1D43') sb.append('a');
                else if (c >= '\u1D47' && c <= '\u1D49') sb.append((char) (c - '\u1D47' + 'b'));
                else if (c == '\u1D4A') sb.append('e');
            }
            else if (c >= '\u1D4D' && c <= '\u1D4F') {
                if (c == '\u1D4D') sb.append('k');
                else if (c == '\u1D4F') sb.append('o');
            }
            else if (c == '\u1D50') sb.append('m');
            else if (c == '\u1D52') sb.append('p');
            else if (c == '\u1D57') sb.append('t');
            else if (c == '\u1D58') sb.append('u');
            else if (c == '\u1D5B') sb.append('v');
            else if (c >= '\u1D9C' && c <= '\u1D9F') {
                if (c == '\u1D9C') sb.append('c');
                else if (c == '\u1D9F') sb.append('f');
            }
            else if (c == '\u1DA0') sb.append('f');
            else if (c == '\u2090') sb.append('a');
            else if (c == '\u2091') sb.append('e');
            else if (c == '\u2092') sb.append('o');
            else if (c == '\u2095') sb.append('h');
            else if (c == '\u2096') sb.append('k');
            else if (c == '\u2097') sb.append('l');
            else if (c == '\u2098') sb.append('m');
            else if (c == '\u2099') sb.append('n');
            else if (c == '\u209A') sb.append('p');
            else if (c == '\u209B') sb.append('s');
            else if (c == '\u209C') sb.append('t');
            else sb.append(c);
        }
        return sb.toString();
    }

    private static boolean shouldFilterLine(String normalized) {
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String kw : FILTER_KEYWORDS) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private static String stripLeadingSymbol(String line) {
        if (line.isEmpty()) return line;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c >= 0xA000 && c <= 0xAFFF) {
                continue;
            }
            if (c == ' ') return line.substring(i + 1);
            if (c != ' ') return line.substring(i);
        }
        return line;
    }

    private static String highlightTarget(String line, String targetName) {
        if (targetName == null || targetName.isEmpty()) return line;
        String highlighted = "\u001b[1;31m" + targetName + "\u001b[0m";
        return line.replace(targetName, highlighted);
    }

    private static List<String> filterAndColorChat(List<String> chatHistory, String targetName) {
        if (chatHistory == null || chatHistory.isEmpty()) return List.of();

        List<String> cleanList = new ArrayList<>();
        for (String line : chatHistory) {
            if (line.contains("\u00BB")) continue;
            if (hasBoxDrawing(line)) continue;

            String stripped = hasYiSymbol(line) ? stripLeadingSymbol(line) : line;

            String normalized = normalizeSmallCaps(stripped);
            if (shouldFilterLine(normalized)) continue;

            String lowerNorm = normalized.toLowerCase(Locale.ROOT);
            if (lowerNorm.contains("casus")
                && targetName != null && !normalized.contains(targetName)) {
                continue;
            }

            if (!normalized.contains(":") && !normalized.contains("->")) continue;

            cleanList.add(highlightTarget(normalized, targetName));
        }

        int maxMessages = Math.max(1, Math.min(50, ActionsManager.webhookMessageCount));
        if (cleanList.size() > maxMessages) {
            return cleanList.subList(cleanList.size() - maxMessages, cleanList.size());
        }
        return cleanList;
    }

    private static List<String> buildChatChunks(List<String> chatHistory, String targetName) {
        List<String> filtered = filterAndColorChat(chatHistory, targetName);
        if (filtered.isEmpty()) return List.of();

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : filtered) {
            if (current.length() + line.length() + 1 > MAX_CHUNK_LENGTH && current.length() > 0) {
                chunks.add(current.toString());
                if (chunks.size() >= MAX_CHAT_CHUNKS) break;
                current.setLength(0);
            }
            if (current.length() > 0) current.append("\n");
            current.append(line);
        }
        if (current.length() > 0 && chunks.size() < MAX_CHAT_CHUNKS) {
            chunks.add(current.toString());
        }

        List<String> wrapped = new ArrayList<>();
        for (String chunk : chunks) {
            wrapped.add("```ansi\n" + chunk + "\n```");
        }
        return wrapped;
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
