package com.smthsmoderation.util;

import com.smthsmoderation.config.ActionsManager;
import java.util.LinkedList;

public class ChatBacklog {

    private static final LinkedList<String> messages = new LinkedList<>();

    public static void push(String message) {
        synchronized (messages) {
            String plain = message.replaceAll("§[0-9a-fklmnor]", "");
            messages.addLast(plain);
            int limit = Math.max(ActionsManager.logMessageCount, 1);
            while (messages.size() > limit) {
                messages.removeFirst();
            }
        }
    }

    public static LinkedList<String> snapshot() {
        synchronized (messages) {
            return new LinkedList<>(messages);
        }
    }

    public static void clear() {
        synchronized (messages) {
            messages.clear();
        }
    }
}
