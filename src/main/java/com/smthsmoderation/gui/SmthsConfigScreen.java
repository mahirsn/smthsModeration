package com.smthsmoderation.gui;

import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.config.ModerationAction;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen (ModMenu / Cloth Config). Organized like a typical
 * multi-category Cloth Config mod: one flat, colored tab per concern
 * (General / Local Logging / Discord Webhook / Actions) with only native
 * Cloth Config widgets — no hand-rolled bevel widgets. The dynamic list of
 * moderation actions isn't a settings tree; each action is a row here that
 * opens its own dedicated {@link ActionEditorScreen}, the same way you'd
 * open a separate editor for one item in a list rather than nesting it.
 */
public class SmthsConfigScreen {

    static final int GREEN = 0xFF55FF55;
    static final int RED = 0xFFFF7777;

    public static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("SmthsModeration"))
                .transparentBackground()
                .setSavingRunnable(ActionsManager::save);

        var config = ActionsManager.config;
        var entries = ConfigEntryBuilder.create();

        var general = builder.getOrCreateCategory(Component.literal("General").withStyle(ChatFormatting.AQUA));
        general.addEntry(entries.startBooleanToggle(Component.literal("Enable Mod"), config.modEnabled)
                .setSaveConsumer(v -> { config.modEnabled = v; ActionsManager.saveConfig(); })
                .build());
        general.addEntry(entries.startBooleanToggle(Component.literal("Shift+Right-Click on Players"), config.enableEntityClick)
                .setSaveConsumer(v -> { config.enableEntityClick = v; ActionsManager.saveConfig(); })
                .build());
        general.addEntry(entries.startBooleanToggle(Component.literal("Shift+Click in Chat"), config.enableChatClick)
                .setSaveConsumer(v -> { config.enableChatClick = v; ActionsManager.saveConfig(); })
                .build());
        general.addEntry(entries.startBooleanToggle(Component.literal("Show History Button"), config.showHistoryButton)
                .setSaveConsumer(v -> { config.showHistoryButton = v; ActionsManager.saveConfig(); })
                .build());
        general.addEntry(entries.startIntField(Component.literal("History Command Limit"), config.historyCommandLimit)
                .setMin(1).setMax(100)
                .setSaveConsumer(v -> { config.historyCommandLimit = v; ActionsManager.saveConfig(); })
                .build());

        var logging = builder.getOrCreateCategory(Component.literal("Local Logging").withStyle(ChatFormatting.GRAY));
        logging.addEntry(entries.startBooleanToggle(Component.literal("Enable Local Logging"), config.enableLocalLogging)
                .setSaveConsumer(v -> { config.enableLocalLogging = v; ActionsManager.saveConfig(); })
                .build());
        logging.addEntry(entries.startIntField(Component.literal("Logged Message Count"), config.logMessageCount)
                .setMin(5).setMax(200)
                .setSaveConsumer(v -> { config.logMessageCount = v; ActionsManager.saveConfig(); })
                .build());

        var webhook = builder.getOrCreateCategory(Component.literal("Discord Webhook").withStyle(ChatFormatting.LIGHT_PURPLE));
        webhook.addEntry(entries.startBooleanToggle(Component.literal("Enable Webhook"), config.enableWebhook)
                .setTooltip(Component.literal("Send moderation embeds to a Discord webhook."))
                .setSaveConsumer(v -> { config.enableWebhook = v; ActionsManager.saveConfig(); })
                .build());
        webhook.addEntry(entries.startStrField(Component.literal("Webhook URL"), config.webhookUrl)
                .setTooltip(Component.literal("Paste your Discord webhook URL here. Leave empty to disable webhook posting."))
                .setSaveConsumer(v -> { config.webhookUrl = v; ActionsManager.saveConfig(); })
                .build());
        webhook.addEntry(entries.startIntField(Component.literal("Webhook Message Count"), config.webhookMessageCount)
                .setMin(5).setMax(50)
                .setTooltip(Component.literal("Number of recent chat messages included in the webhook embed."))
                .setSaveConsumer(v -> { config.webhookMessageCount = v; ActionsManager.saveConfig(); })
                .build());

        var actions = builder.getOrCreateCategory(Component.literal("Actions").withStyle(ChatFormatting.GOLD));
        for (ModerationAction action : ActionsManager.actions) {
            actions.addEntry(new ButtonEntry(Component.literal(action.type), action.getColor(), () ->
                    Minecraft.getInstance().gui.setScreen(ActionEditorScreen.build(Minecraft.getInstance().gui.screen(), parent, action))));
        }
        actions.addEntry(new ButtonEntry(Component.literal("+ Add New Action"), GREEN, () -> {
            ActionsManager.actions.add(new ModerationAction("New-" + System.nanoTime(), "/command %player%", 0xFF555555, ""));
            Minecraft.getInstance().gui.setScreen(build(parent));
        }));

        return builder.build();
    }
}
