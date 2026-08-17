package com.smthsmoderation.gui;

import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.config.CommandVariable;
import com.smthsmoderation.config.ModerationAction;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dedicated editor for a single {@link ModerationAction} — mirrors how
 * ChatPlus opens a separate Cloth Config screen per chat tab/window instead
 * of nesting a whole item's fields inside the main settings tree.
 */
public class ActionEditorScreen {

    public static Screen build(Screen configScreen, Screen grandparent, ModerationAction action) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(configScreen)
                .setTitle(Component.literal(action.type))
                .transparentBackground()
                .setSavingRunnable(ActionsManager::save);
        builder.setGlobalizedExpanded(true);
        var entries = ConfigEntryBuilder.create();

        var actionCategory = builder.getOrCreateCategory(Component.literal("Action").withStyle(ChatFormatting.GOLD));
        actionCategory.addEntry(entries.startStrField(Component.literal("Name"), action.type)
                .setSaveConsumer(v -> action.type = v)
                .build());
        actionCategory.addEntry(entries.startStrField(Component.literal("Command Template"), action.commandTemplate)
                .setTooltip(Component.literal("Use %player% and %<variable>% placeholders, e.g. /ban %player% %duration% %reason%"))
                .setSaveConsumer(v -> action.commandTemplate = v)
                .build());
        actionCategory.addEntry(entries.startStrField(Component.literal("Button Color (hex)"), String.format("#%06X", 0xFFFFFF & action.buttonColor))
                .setSaveConsumer(v -> {
                    try {
                        action.buttonColor = 0xFF000000 | Integer.parseInt(v.replace("#", "").trim(), 16);
                    } catch (NumberFormatException ignored) {
                        // Keep the previous color on invalid input.
                    }
                })
                .build());
        actionCategory.addEntry(entries.startStrField(Component.literal("Description"), action.description)
                .setSaveConsumer(v -> action.description = v)
                .build());
        actionCategory.addEntry(entries.startBooleanToggle(Component.literal("Visible"), action.isVisible)
                .setSaveConsumer(v -> action.isVisible = v)
                .build());
        actionCategory.addEntry(entries.startBooleanToggle(Component.literal("Requires Confirmation"), action.requiresConfirmation)
                .setSaveConsumer(v -> action.requiresConfirmation = v)
                .build());
        actionCategory.addEntry(entries.startBooleanToggle(Component.literal("Send to Webhook"), action.sendWebhook)
                .setTooltip(Component.literal("Send this action's execution to the Discord webhook."))
                .setSaveConsumer(v -> action.sendWebhook = v)
                .build());
        actionCategory.addEntry(new ButtonEntry(Component.literal("Delete Action"), SmthsConfigScreen.RED, () -> {
            ActionsManager.actions.remove(action);
            Minecraft.getInstance().gui.setScreen(SmthsConfigScreen.build(grandparent));
        }));

        var multiplier = builder.getOrCreateCategory(Component.literal("Penalty Multiplier").withStyle(ChatFormatting.RED));
        multiplier.addEntry(entries.startBooleanToggle(Component.literal("Enable Multiplier"), action.smartMultiplierEnabled)
                .setSaveConsumer(v -> action.smartMultiplierEnabled = v)
                .build());
        multiplier.addEntry(entries.startStrField(Component.literal("Keyword (e.g. muted)"), action.multiplierKeyword)
                .setSaveConsumer(v -> action.multiplierKeyword = v)
                .build());
        multiplier.addEntry(entries.startStrField(Component.literal("Base Penalty Time (e.g. 30m)"), action.basePenaltyTime)
                .setSaveConsumer(v -> action.basePenaltyTime = v)
                .build());
        multiplier.addEntry(entries.startDoubleField(Component.literal("Multiplier Step (per infraction)"), action.multiplierStep)
                .setMin(0.01).setMax(10.0)
                .setSaveConsumer(v -> action.multiplierStep = v)
                .build());
        multiplier.addEntry(entries.startDoubleField(Component.literal("Max Multiplier"), action.multiplierMax)
                .setMin(1.0).setMax(100.0)
                .setSaveConsumer(v -> action.multiplierMax = v)
                .build());
        multiplier.addEntry(entries.startStrField(Component.literal("Reduction Keyword"), action.reductionKeyword)
                .setSaveConsumer(v -> action.reductionKeyword = v)
                .build());
        multiplier.addEntry(entries.startStrField(Component.literal("Target Variable"), action.targetVariableForPM)
                .setSaveConsumer(v -> action.targetVariableForPM = v)
                .build());

        var variables = builder.getOrCreateCategory(Component.literal("Variables").withStyle(ChatFormatting.YELLOW));
        for (int i = 0; i < action.variables.size(); i++) {
            variables.addEntry(buildVariableEntry(entries, action, i, configScreen, grandparent));
        }
        variables.addEntry(new ButtonEntry(Component.literal("+ Add Variable"), SmthsConfigScreen.GREEN, () -> {
            action.variables.add(new CommandVariable("new_var" + (action.variables.size() + 1), "", false));
            Minecraft.getInstance().gui.setScreen(build(configScreen, grandparent, action));
        }));

        return builder.build();
    }

    private static me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry buildVariableEntry(
            ConfigEntryBuilder entries, ModerationAction action, int index, Screen configScreen, Screen grandparent) {
        CommandVariable variable = action.variables.get(index);
        var sub = entries.startSubCategory(Component.literal(variable.name.isBlank() ? "(unnamed)" : variable.name));
        sub.add(entries.startStrField(Component.literal("Name"), variable.name)
                .setSaveConsumer(v -> variable.name = v).build());
        sub.add(entries.startStrField(Component.literal("Presets (CSV)"), variable.presets)
                .setSaveConsumer(v -> variable.presets = v).build());
        sub.add(entries.startBooleanToggle(Component.literal("Required"), variable.isRequired)
                .setSaveConsumer(v -> variable.isRequired = v).build());
        sub.add(new ButtonEntry(Component.literal("Remove Variable"), SmthsConfigScreen.RED, () -> {
            action.variables.remove(index);
            Minecraft.getInstance().gui.setScreen(build(configScreen, grandparent, action));
        }));
        return sub.build();
    }
}
