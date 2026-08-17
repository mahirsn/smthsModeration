package com.smthsmoderation.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe bounded buffer of recent chat lines, used to give the local
 * log and Discord webhook some context around a punishment. The capacity is
 * a fixed constant rather than derived from the log/webhook message-count
 * settings — those are two independently configurable "how many do I want"
 * numbers, and coupling the buffer size to a formula of both created a class
 * of bugs where one setting silently truncated the other's history.
 */
public final class ChatBacklog {

    private static final int CAPACITY = 200;
    private static final Deque<String> messages = new ArrayDeque<>();

    private ChatBacklog() {
    }

    public static void push(String message) {
        String plain = message.replaceAll("§[0-9a-fklmnor]", "");
        synchronized (messages) {
            messages.addLast(plain);
            while (messages.size() > CAPACITY) {
                messages.removeFirst();
            }
        }
    }

    /** Up to {@code count} most recent messages, oldest first. */
    public static List<String> snapshot(int count) {
        synchronized (messages) {
            int skip = Math.max(0, messages.size() - count);
            return messages.stream().skip(skip).toList();
        }
    }

    public static void clear() {
        synchronized (messages) {
            messages.clear();
        }
    }
}
