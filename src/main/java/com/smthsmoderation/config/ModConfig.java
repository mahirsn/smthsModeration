package com.smthsmoderation.config;

/** General mod settings, persisted as {@code config/smthsmoderation_config.json}. */
public class ModConfig {
    public boolean modEnabled = true;
    public boolean enableEntityClick = true;
    public boolean enableChatClick = true;
    public boolean showHistoryButton = true;
    public int historyCommandLimit = 10;
    public boolean enableLocalLogging = true;
    public int logMessageCount = 30;
    public boolean enableWebhook = true;
    public String webhookUrl = "";
    public int webhookMessageCount = 30;
}
