package com.smthsmoderation.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HistoryTracker {

    private static final Map<String, List<HistoryEntry>> history = new LinkedHashMap<>();
    private static final Pattern MOD_ACTION = Pattern.compile(
        "(?i)(?:\\[.+?\\]\\s*)?(\\w{3,16})\\s+(?:was\\s+)?(?:permanently\\s+|temporarily\\s+)?(banned|kicked|warned|muted|tempbanned|tempmuted|teleported|invsee|tp(?:ed|hered)?)" +
        "(?:\\s+(?:by|from)\\s+(?:the\\s+)?(?:server\\s+)?(\\w{3,16}))?" +
        "(?:\\s+(?:for|reason[.:]?)\\s+(.+))?"
    );

    public static void onChatMessage(String raw) {
        String plain = raw.replaceAll("§[0-9a-fklmnor]", "");
        Matcher m = MOD_ACTION.matcher(plain);
        if (m.find()) {
            String player = m.group(1);
            String action = m.group(2);
            String staff = m.group(3);
            String reason = m.group(4) != null ? m.group(4).trim() : "";
            history.computeIfAbsent(player, k -> new ArrayList<>())
                .add(new HistoryEntry(raw, action, staff != null ? staff : "", reason, System.currentTimeMillis()));
        }
    }

    public static List<HistoryEntry> getHistory(String player) {
        return history.getOrDefault(player, Collections.emptyList());
    }

    public static List<HistoryEntry> getAllEntries() {
        return history.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static void clear() {
        history.clear();
    }

    public static class HistoryEntry {
        public final String rawText;
        public final String action;
        public final String staff;
        public final String reason;
        public final long timestamp;

        public HistoryEntry(String rawText, String action, String staff, String reason, long timestamp) {
            this.rawText = rawText;
            this.action = action;
            this.staff = staff;
            this.reason = reason;
            this.timestamp = timestamp;
        }

        public String formatted() {
            String ts = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(timestamp));
            String staffStr = staff.isEmpty() ? "" : " §7by " + staff;
            String reasonStr = reason.isEmpty() ? "" : " §7(" + reason + ")";
            return "§8[" + ts + "] §c" + action.toUpperCase() + staffStr + reasonStr;
        }
    }
}
