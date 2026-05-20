package com.smthsmoderation.gui;

import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.config.CommandVariable;
import com.smthsmoderation.config.ModerationAction;
import com.smthsmoderation.util.GuiUtil;
import com.smthsmoderation.util.HistoryTracker;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModerationScreen extends BaseOwoScreen<FlowLayout> {

    private final String playerName;
    private final Identifier skinTexture;
    private final List<ModerationAction> configActions = new ArrayList<>();
    private int selectedIndex = -1;

    private FlowLayout panel;
    private ModerationAction currentAction;
    private ButtonComponent executeBtn;
    private FlowLayout confirmRow;
    private FlowLayout variablesSection;
    private FlowLayout previewSection;
    private FlowLayout historyContent;
    private boolean showingConfirmation = false;

    private final Map<String, TextBoxComponent> varFields = new HashMap<>();
    private final List<ButtonComponent> actionButtons = new ArrayList<>();

    public ModerationScreen(String playerName, Identifier skinTexture) {
        this.playerName = playerName;
        this.skinTexture = skinTexture;
        loadActions();
    }

    private void loadActions() {
        configActions.clear();
        configActions.addAll(ActionsManager.actions);
        configActions.removeIf(a -> !a.isVisible);
        if (!configActions.isEmpty()) {
            selectedIndex = 0;
            currentAction = configActions.get(0);
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT);
        root.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

        panel = UIContainers.verticalFlow(Sizing.fixed(320), Sizing.content());
        panel.surface(Surface.flat(GuiUtil.PANEL_BG));
        panel.padding(Insets.of(12));
        panel.gap(6);

        buildHeader(panel);
        buildActionButtons(panel);

        buildVariableFields(panel);

        previewSection = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
        panel.child(previewSection);

        confirmRow = createConfirmRow();
        buildExecuteButton(panel);
        buildHistoryButton(panel);

        historyContent = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
        historyContent.gap(2);
        panel.child(historyContent);

        root.child(panel);
        refreshVariables();
        updatePreview();
        updateExecuteButton();
    }

    private void buildHeader(FlowLayout panel) {
        FlowLayout header = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        header.gap(8);

        header.child(UIComponents.texture(skinTexture, 8, 8, 8, 8, 64, 64)
            .sizing(Sizing.fixed(22), Sizing.fixed(22)));

        ButtonComponent nameBtn = UIComponents.button(
            Text.literal("§lModerate: " + playerName),
            button -> MinecraftClient.getInstance().setScreen(new PlayerSelectorScreen())
        );
        nameBtn.renderer(GuiUtil.modernButton(GuiUtil.DARK_BG, GuiUtil.DARK_BG_HOVER, GuiUtil.DISABLED));
        nameBtn.sizing(Sizing.fill(), Sizing.content());
        header.child(nameBtn);

        panel.child(header);
    }

    private void buildActionButtons(FlowLayout panel) {
        if (configActions.isEmpty()) return;

        var actionFlow = UIContainers.ltrTextFlow(Sizing.fill(100), Sizing.content());
        actionFlow.gap(4);
        actionFlow.margins(Insets.vertical(2));

        actionButtons.clear();
        for (int i = 0; i < configActions.size(); i++) {
            int idx = i;
            ModerationAction action = configActions.get(i);
            int color = action.getColor();
            int hover = action.getHoverColor();

            ButtonComponent btn = UIComponents.button(
                Text.literal(action.type),
                b -> selectAction(idx)
            );
            btn.renderer(GuiUtil.modernButton(color, hover, GuiUtil.DISABLED));

            var tr = MinecraftClient.getInstance().textRenderer;
            int btnW = tr.getWidth(Text.literal(action.type)) + 12;
            btn.sizing(Sizing.fixed(btnW), Sizing.fixed(20));
            btn.margins(Insets.of(2));

            if (action.description != null && !action.description.isEmpty()) {
                btn.tooltip(Text.literal(action.description));
            }

            actionFlow.child(btn);
            actionButtons.add(btn);
        }

        updateActionSelection();
        panel.child(actionFlow);
    }

    private void updateActionSelection() {
        for (int i = 0; i < actionButtons.size(); i++) {
            ButtonComponent btn = actionButtons.get(i);
            if (i == selectedIndex) {
                btn.renderer(GuiUtil.outlinedButton(
                    configActions.get(i).getColor(),
                    configActions.get(i).getHoverColor(),
                    GuiUtil.DISABLED,
                    0xFFFFFFFF
                ));
            } else {
                btn.renderer(GuiUtil.modernButton(
                    configActions.get(i).getColor(),
                    configActions.get(i).getHoverColor(),
                    GuiUtil.DISABLED
                ));
            }
        }
    }

    private void buildVariableFields(FlowLayout panel) {
        variablesSection = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
        variablesSection.gap(3);
        panel.child(variablesSection);
    }

    private void refreshVariables() {
        variablesSection.clearChildren();
        varFields.clear();

        if (currentAction == null) return;

        for (CommandVariable var : currentAction.variables) {
            variablesSection.child(UIComponents.label(Text.literal("§7" + var.name + ":")));

            TextBoxComponent field = UIComponents.textBox(Sizing.fixed(120), "");
            field.setMaxLength(64);
            field.onChanged().subscribe(text -> { updatePreview(); updateExecuteButton(); });
            variablesSection.child(field);
            varFields.put(var.name, field);

            List<String> presetList = var.getPresetList();
            if (!presetList.isEmpty()) {
                FlowLayout chipRow = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
                chipRow.gap(4);
                for (String preset : presetList) {
                    ButtonComponent chip = UIComponents.button(Text.literal(preset), b -> {
                        field.text(preset);
                        updatePreview();
                        updateExecuteButton();
                    });
                    chip.renderer(GuiUtil.modernButton(GuiUtil.DARK_BG, GuiUtil.DARK_BG_HOVER, GuiUtil.DISABLED));
                    chip.sizing(Sizing.content(), Sizing.fixed(16));
                    chipRow.child(chip);
                }
                variablesSection.child(chipRow);
            }
        }
    }

    private void updatePreview() {
        previewSection.clearChildren();
        if (currentAction == null) return;

        String preview = currentAction.commandTemplate
            .replaceAll("(?i)%player%", Matcher.quoteReplacement(playerName));

        for (Map.Entry<String, TextBoxComponent> e : varFields.entrySet()) {
            String val = e.getValue().getText().trim();
            if (!val.isEmpty()) {
                preview = preview.replaceAll(
                    "(?i)%" + Pattern.quote(e.getKey()) + "%",
                    Matcher.quoteReplacement(val)
                );
            }
        }
        preview = preview.replaceAll("%[^%]+%", "...");

        previewSection.child(UIComponents.label(Text.literal("§7Preview: §f" + preview)));
    }

    private void selectAction(int index) {
        if (index < 0 || index >= configActions.size()) return;
        selectedIndex = index;
        currentAction = configActions.get(index);
        showingConfirmation = false;

        updateActionSelection();
        refreshVariables();
        updatePreview();
        updateExecuteButton();
    }

    private FlowLayout createConfirmRow() {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.gap(6);
        row.child(UIComponents.label(Text.literal("§eConfirm action?")));
        ButtonComponent yesBtn = UIComponents.button(Text.literal("§aYes"), b -> {
            showingConfirmation = false;
            executeCurrentAction();
        });
        yesBtn.renderer(GuiUtil.modernButton(GuiUtil.GREEN, GuiUtil.GREEN_HOVER, GuiUtil.DISABLED));
        yesBtn.sizing(Sizing.fixed(50), Sizing.fixed(20));
        row.child(yesBtn);
        ButtonComponent noBtn = UIComponents.button(Text.literal("§cNo"), b -> {
            showingConfirmation = false;
            updateExecuteButton();
        });
        noBtn.renderer(GuiUtil.modernButton(GuiUtil.RED, GuiUtil.RED_HOVER, GuiUtil.DISABLED));
        noBtn.sizing(Sizing.fixed(50), Sizing.fixed(20));
        row.child(noBtn);
        return row;
    }

    private void buildExecuteButton(FlowLayout panel) {
        executeBtn = UIComponents.button(
            Text.literal("§lExecute"),
            b -> {
                if (currentAction != null && currentAction.requiresConfirmation && !showingConfirmation) {
                    showingConfirmation = true;
                    updateExecuteButton();
                } else {
                    showingConfirmation = false;
                    executeCurrentAction();
                }
            }
        );
        executeBtn.renderer(GuiUtil.modernButton(GuiUtil.GREEN, GuiUtil.GREEN_HOVER, GuiUtil.DISABLED));
        executeBtn.sizing(Sizing.fill(), Sizing.fixed(22));
        panel.child(executeBtn);
    }

    private void buildHistoryButton(FlowLayout panel) {
        if (!ActionsManager.showHistoryButton) return;
        ButtonComponent historyBtn = UIComponents.button(
            Text.literal("§7Show History"),
            b -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand("history " + playerName + " " + ActionsManager.historyCommandLimit);
                }
            }
        );
        historyBtn.renderer(GuiUtil.modernButton(GuiUtil.DARK_BG, GuiUtil.DARK_BG_HOVER, GuiUtil.DISABLED));
        historyBtn.sizing(Sizing.fill(), Sizing.fixed(20));
        historyBtn.tooltip(Text.literal("§7View LiteBans history for §f" + playerName));
        panel.child(historyBtn);
    }

    private void updateExecuteButton() {
        if (currentAction == null || executeBtn == null || confirmRow == null) return;

        boolean canExecute = true;
        for (CommandVariable var : currentAction.variables) {
            if (var.isRequired) {
                TextBoxComponent field = varFields.get(var.name);
                if (field == null || field.getText().trim().isEmpty()) {
                    canExecute = false;
                    break;
                }
            }
        }

        if (showingConfirmation) {
            executeBtn.active = false;
            if (!confirmRow.hasParent()) {
                int idx = panel.children().indexOf(executeBtn);
                if (idx >= 0) panel.child(idx, confirmRow);
            }
        } else {
            executeBtn.active = canExecute;
            if (confirmRow.hasParent()) {
                panel.removeChild(confirmRow);
            }
        }
    }

    private void executeCurrentAction() {
        if (currentAction == null || MinecraftClient.getInstance().player == null) return;

        String command = currentAction.commandTemplate
            .replaceAll("(?i)%player%", Matcher.quoteReplacement(playerName));

        for (Map.Entry<String, TextBoxComponent> e : varFields.entrySet()) {
            String val = e.getValue().getText().trim();
            command = command.replaceAll(
                "(?i)%" + Pattern.quote(e.getKey()) + "%",
                Matcher.quoteReplacement(val)
            );
        }
        command = command.replaceAll("%[^%]+%", "");

        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(
            command.startsWith("/") ? command.substring(1) : command
        );
    }

    private void refreshHistory() {
        historyContent.clearChildren();
        var entries = HistoryTracker.getHistory(playerName);
        if (entries.isEmpty()) {
            historyContent.child(UIComponents.label(Text.literal("§8No history data yet")));
        } else {
            for (int i = Math.max(0, entries.size() - 10); i < entries.size(); i++) {
                historyContent.child(UIComponents.label(Text.literal(entries.get(i).formatted())));
            }
        }
    }
}
