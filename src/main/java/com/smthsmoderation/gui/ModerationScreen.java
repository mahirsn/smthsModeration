package com.smthsmoderation.gui;

import com.smthsmoderation.action.CommandTemplate;
import com.smthsmoderation.action.PenaltyMultiplier;
import com.smthsmoderation.action.TimeUtils;
import com.smthsmoderation.chat.ChatBacklog;
import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.config.CommandVariable;
import com.smthsmoderation.config.ModerationAction;
import com.smthsmoderation.log.PunishmentLogger;
import com.smthsmoderation.webhook.DiscordWebhook;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Main working screen: pick an action, fill its variables, execute, review history. */
public class ModerationScreen extends Screen {

    private static ModerationScreen instance;

    private static final int COLUMN_WIDTH = 230;
    private static final int COLUMN_GAP = 16;
    private static final int PANEL_HEIGHT = 340;
    private static final int PADDING = 14;
    private static final int HISTORY_LOG_HEIGHT = 176;

    private final String playerName;
    private final UUID playerUuid;
    private final List<ModerationAction> configActions = new ArrayList<>();
    private int selectedIndex = -1;
    private ModerationAction currentAction;

    private final Map<String, EditBox> varFields = new HashMap<>();
    private final List<AbstractWidget> leftDynamicWidgets = new ArrayList<>();
    private record TextLine(String text, int x, int y, int color) {
    }
    private final List<TextLine> leftLabels = new ArrayList<>();
    private final List<int[]> leftSeparators = new ArrayList<>();
    private int previewY;
    private ActionButton executeButton;
    private ActionButton confirmYes;
    private ActionButton confirmNo;
    private boolean showingConfirmation = false;

    private boolean awaitingHistory = false;
    private final List<String> historyLines = new ArrayList<>();
    private final Map<String, Integer> infractionCounts = new HashMap<>();
    private int historyScrollOffset = 0;

    private int leftX;
    private int rightX;
    private int topY;
    private int historyLogTop;

    public ModerationScreen(String playerName, UUID playerUuid) {
        super(Component.literal("Moderate " + playerName));
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        instance = this;
        configActions.addAll(ActionsManager.actions.stream().filter(a -> a.isVisible).toList());
        if (!configActions.isEmpty()) {
            selectedIndex = 0;
            currentAction = configActions.get(0);
        }
    }

    @Override
    protected void init() {
        leftX = (this.width - (COLUMN_WIDTH * 2 + COLUMN_GAP)) / 2;
        rightX = leftX + COLUMN_WIDTH + COLUMN_GAP;
        topY = (this.height - PANEL_HEIGHT) / 2;

        rebuildLeftColumn();
    }

    @Override
    public void onClose() {
        awaitingHistory = false;
        infractionCounts.clear();
        instance = null;
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        GuiUtil.drawPanel(graphics, leftX, topY, COLUMN_WIDTH, PANEL_HEIGHT);
        GuiUtil.drawPanel(graphics, rightX, topY, COLUMN_WIDTH, PANEL_HEIGHT);

        graphics.blit(RenderPipelines.GUI_TEXTURED, PlayerSkins.resolve(playerUuid), leftX + PADDING, topY + PADDING, 8, 8, 20, 20, 8, 8, 64, 64);
        graphics.text(this.font, playerName, leftX + PADDING + 28, topY + PADDING + 5, GuiUtil.TEXT_PRIMARY, false);

        for (int[] sep : leftSeparators) {
            graphics.fill(sep[0], sep[1], sep[0] + sep[2], sep[1] + 1, GuiUtil.PANEL_BORDER);
        }
        for (TextLine label : leftLabels) {
            graphics.text(this.font, label.text(), label.x(), label.y(), label.color(), false);
        }
        if (currentAction != null) {
            String preview = CommandTemplate.preview(currentAction.commandTemplate, playerName, currentVariableValues());
            graphics.text(this.font, "Preview", leftX + PADDING, previewY, GuiUtil.TEXT_MUTED, false);
            graphics.text(this.font, preview, leftX + PADDING, previewY + 10, GuiUtil.TEXT_PRIMARY, false);
        }

        graphics.text(this.font, "History", rightX + PADDING, topY + PADDING - 2, GuiUtil.TEXT_PRIMARY, false);
        renderHistoryLog(graphics);
        renderMultiplierResults(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean overHistory = mouseX >= rightX && mouseX <= rightX + COLUMN_WIDTH
                && mouseY >= historyLogTop && mouseY <= historyLogTop + HISTORY_LOG_HEIGHT;
        if (overHistory) {
            int maxOffset = Math.max(0, historyLines.size() - (HISTORY_LOG_HEIGHT / this.font.lineHeight));
            historyScrollOffset = Math.clamp(historyScrollOffset - (int) Math.signum(scrollY), 0, maxOffset);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // --- Left column: header, actions, variables, execute, history button ---

    private void rebuildLeftColumn() {
        leftDynamicWidgets.forEach(this::removeWidget);
        leftDynamicWidgets.clear();
        varFields.clear();
        leftLabels.clear();
        leftSeparators.clear();

        int y = topY + PADDING + 24;
        addWidget(new ActionButton(this.font, leftX + PADDING, y, COLUMN_WIDTH - PADDING * 2, 16,
                Component.literal("Change target"), GuiUtil.SURFACE,
                () -> Minecraft.getInstance().gui.setScreen(new PlayerSelectorScreen())), y, 16);
        y += 16;
        y = separator(y, 10);

        y = layoutActionButtons(y);
        y = separator(y, 10);

        y = layoutVariableFields(y);
        y += 4;

        previewY = y;
        y += 22;
        y = separator(y, 10);

        y = layoutExecuteArea(y);
        y += 8;

        if (ActionsManager.config.showHistoryButton) {
            addWidget(new ActionButton(this.font, leftX + PADDING, y, COLUMN_WIDTH - PADDING * 2, 18,
                    Component.literal("Show History"), GuiUtil.SURFACE, this::requestHistory), y, 18);
        }

        historyLogTop = topY + PADDING + 20;
        updateExecuteButtonState();
    }

    private int separator(int y, int gapAfter) {
        leftSeparators.add(new int[]{leftX + PADDING, y + 4, COLUMN_WIDTH - PADDING * 2});
        return y + 4 + 1 + gapAfter;
    }

    private int layoutActionButtons(int startY) {
        int x = leftX + PADDING;
        int y = startY;
        int rowHeight = 20;
        for (int i = 0; i < configActions.size(); i++) {
            ModerationAction action = configActions.get(i);
            int width = this.font.width(action.type) + 14;
            if (x + width > leftX + COLUMN_WIDTH - PADDING) {
                x = leftX + PADDING;
                y += rowHeight + 5;
            }
            int index = i;
            ActionButton button = new ActionButton(this.font, x, y, width, rowHeight - 2,
                    Component.literal(action.type), action.getColor(), () -> selectAction(index));
            button.setSelected(i == selectedIndex);
            if (action.description != null && !action.description.isEmpty()) {
                button.setTooltip(Tooltip.create(Component.literal(action.description)));
            }
            addWidget(button, y, rowHeight);
            x += width + 5;
        }
        return y + rowHeight;
    }

    private int layoutVariableFields(int startY) {
        if (currentAction == null) return startY;
        int y = startY;
        for (CommandVariable variable : currentAction.variables) {
            leftLabels.add(new TextLine(variable.name, leftX + PADDING, y, GuiUtil.TEXT_MUTED));
            y += 11;
            EditBox field = new EditBox(this.font, leftX + PADDING, y, 130, 16, Component.literal(variable.name));
            field.setMaxLength(64);
            field.setResponder(text -> updateExecuteButtonState());
            addWidget(field, y, 16);
            varFields.put(variable.name, field);
            y += 20;

            List<String> presets = variable.getPresetList();
            boolean showPenaltyChip = currentAction.smartMultiplierEnabled
                    && currentAction.targetVariableForPM.equalsIgnoreCase(variable.name);
            if (!presets.isEmpty() || showPenaltyChip) {
                int chipX = leftX + PADDING;
                for (String preset : presets) {
                    int chipWidth = this.font.width(preset) + 8;
                    ActionButton chip = new ActionButton(this.font, chipX, y, chipWidth, 14,
                            Component.literal(preset), GuiUtil.SURFACE, () -> field.setValue(preset));
                    addWidget(chip, y, 14);
                    chipX += chipWidth + 4;
                }
                if (showPenaltyChip) {
                    int chipWidth = this.font.width("PM") + 8;
                    ActionButton pmChip = new ActionButton(this.font, chipX, y, chipWidth, 14,
                            Component.literal("PM"), 0xFFB33A3A, () -> applyPenaltyMultiplier(field));
                    addWidget(pmChip, y, 14);
                }
                y += 19;
            }
            y += 4;
        }
        return y;
    }

    private void applyPenaltyMultiplier(EditBox field) {
        int count = infractionCounts.getOrDefault(currentAction.type, 0);
        long minutes = PenaltyMultiplier.applyToBaseDuration(currentAction.basePenaltyTime, count,
                currentAction.multiplierStep, currentAction.multiplierMax);
        if (minutes > 0) field.setValue(TimeUtils.formatMinutes(minutes));
        updateExecuteButtonState();
    }

    private int layoutExecuteArea(int startY) {
        executeButton = new ActionButton(this.font, leftX + PADDING, startY, COLUMN_WIDTH - PADDING * 2, 22,
                Component.literal("Execute"), 0xFF3E7D44, this::onExecutePressed);
        addWidget(executeButton, startY, 22);

        int halfWidth = (COLUMN_WIDTH - PADDING * 2 - 6) / 2;
        confirmYes = new ActionButton(this.font, leftX + PADDING, startY, halfWidth, 22,
                Component.literal("Confirm"), 0xFF3E7D44, () -> {
            showingConfirmation = false;
            executeCurrentAction();
            updateExecuteButtonState();
        });
        confirmNo = new ActionButton(this.font, leftX + PADDING + halfWidth + 6, startY, halfWidth, 22,
                Component.literal("Cancel"), 0xFFB33A3A, () -> {
            showingConfirmation = false;
            updateExecuteButtonState();
        });
        addWidget(confirmYes, startY, 22);
        addWidget(confirmNo, startY, 22);
        return startY + 22;
    }

    private int addWidget(AbstractWidget widget, int y, int height) {
        leftDynamicWidgets.add(widget);
        this.addRenderableWidget(widget);
        return y;
    }

    private void selectAction(int index) {
        if (index < 0 || index >= configActions.size()) return;
        selectedIndex = index;
        currentAction = configActions.get(index);
        showingConfirmation = false;
        infractionCounts.clear();
        rebuildLeftColumn();
    }

    private void onExecutePressed() {
        if (currentAction == null) return;
        if (currentAction.requiresConfirmation && !showingConfirmation) {
            showingConfirmation = true;
        } else {
            showingConfirmation = false;
            executeCurrentAction();
        }
        updateExecuteButtonState();
    }

    private void updateExecuteButtonState() {
        if (executeButton == null) return;
        boolean allRequiredFilled = currentAction != null && currentAction.variables.stream()
                .filter(v -> v.isRequired)
                .allMatch(v -> varFields.containsKey(v.name) && !varFields.get(v.name).getValue().isBlank());

        executeButton.visible = !showingConfirmation;
        executeButton.active = !showingConfirmation && allRequiredFilled;
        confirmYes.visible = showingConfirmation;
        confirmNo.visible = showingConfirmation;
    }

    private Map<String, String> currentVariableValues() {
        Map<String, String> values = new HashMap<>();
        varFields.forEach((name, field) -> values.put(name, field.getValue().trim()));
        return values;
    }

    private void executeCurrentAction() {
        if (currentAction == null) return;
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;

        Map<String, String> values = currentVariableValues();
        String command = CommandTemplate.fill(currentAction.commandTemplate, playerName, values);
        connection.sendCommand(CommandTemplate.stripLeadingSlash(command));

        if (ActionsManager.config.enableLocalLogging) {
            try {
                var logDir = FabricLoader.getInstance().getGameDir().resolve("smthsmoderations-logs");
                PunishmentLogger.write(logDir, playerName, currentAction.type,
                        values.getOrDefault("duration", ""), values.getOrDefault("reason", ""),
                        command, ChatBacklog.snapshot(ActionsManager.config.logMessageCount));
            } catch (IOException e) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§c[SmthsModeration] Log write failed: " + e.getMessage()));
                }
            }
        }

        if (ActionsManager.config.enableWebhook && currentAction.sendWebhook) {
            DiscordWebhook.sendEmbed(playerName, currentAction.type, values.getOrDefault("duration", ""),
                    values.getOrDefault("reason", ""), command, ChatBacklog.snapshot(ActionsManager.config.webhookMessageCount));
        }
    }

    // --- History panel (right column) ---

    private void requestHistory() {
        awaitingHistory = true;
        infractionCounts.clear();
        historyLines.clear();
        historyLines.add("Fetching history for " + playerName + "...");
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand("history " + playerName + " " + ActionsManager.config.historyCommandLimit);
        }
    }

    public static void appendHistoryLine(String rawMessage) {
        if (instance == null || !instance.awaitingHistory) return;
        instance.historyLines.add(rawMessage);

        String lower = rawMessage.toLowerCase(Locale.ROOT);
        for (ModerationAction action : instance.configActions) {
            if (!action.smartMultiplierEnabled) continue;
            String reduceKw = action.reductionKeyword.toLowerCase(Locale.ROOT);
            String addKw = action.multiplierKeyword.toLowerCase(Locale.ROOT);
            if (!reduceKw.isEmpty() && lower.contains(reduceKw)) {
                instance.infractionCounts.merge(action.type, -1, Integer::sum);
            } else if (!addKw.isEmpty() && lower.contains(addKw)) {
                instance.infractionCounts.merge(action.type, 1, Integer::sum);
            }
        }
    }

    private void renderHistoryLog(GuiGraphicsExtractor graphics) {
        int lineHeight = this.font.lineHeight + 2;
        int visibleLines = HISTORY_LOG_HEIGHT / lineHeight;
        graphics.enableScissor(rightX + PADDING, historyLogTop, rightX + COLUMN_WIDTH - PADDING, historyLogTop + HISTORY_LOG_HEIGHT);
        int end = Math.min(historyLines.size(), historyScrollOffset + visibleLines);
        for (int i = historyScrollOffset; i < end; i++) {
            int y = historyLogTop + (i - historyScrollOffset) * lineHeight;
            graphics.text(this.font, historyLines.get(i), rightX + PADDING, y, GuiUtil.TEXT_MUTED, false);
        }
        graphics.disableScissor();
    }

    private void renderMultiplierResults(GuiGraphicsExtractor graphics) {
        int y = historyLogTop + HISTORY_LOG_HEIGHT + 10;
        graphics.fill(rightX + PADDING, y - 6, rightX + COLUMN_WIDTH - PADDING, y - 5, GuiUtil.PANEL_BORDER);

        if (currentAction == null || !currentAction.smartMultiplierEnabled || currentAction.multiplierKeyword.isEmpty()) {
            graphics.text(this.font, "No multipliers active", rightX + PADDING, y, GuiUtil.TEXT_MUTED, false);
            return;
        }
        int count = infractionCounts.getOrDefault(currentAction.type, 0);
        if (count == 0) {
            graphics.text(this.font, "No multipliers active", rightX + PADDING, y, GuiUtil.TEXT_MUTED, false);
            return;
        }
        double multiplier = PenaltyMultiplier.compute(count, currentAction.multiplierStep, currentAction.multiplierMax);
        long totalMinutes = TimeUtils.multiplyMinutes(TimeUtils.parseMinutes(currentAction.basePenaltyTime), multiplier);
        String line = currentAction.type + "  x" + String.format(Locale.ROOT, "%.2f", multiplier)
                + "  (" + currentAction.basePenaltyTime + " × " + count + " = " + TimeUtils.formatMinutes(totalMinutes) + ")";
        graphics.text(this.font, line, rightX + PADDING, y, GuiUtil.ACCENT, false);
    }
}
