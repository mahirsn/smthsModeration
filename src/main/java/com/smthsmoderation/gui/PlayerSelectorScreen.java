package com.smthsmoderation.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Search + click-to-select list of online players; the first of the mod's three entry channels. */
public class PlayerSelectorScreen extends Screen {

    private static final int PANEL_WIDTH = 270;
    private static final int PANEL_HEIGHT = 320;
    private static final int PADDING = 14;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_GAP = 3;
    private static final int VISIBLE_ROWS = 8;

    private record PlayerData(String name, UUID uuid) {
    }

    private final List<PlayerData> allPlayers = new ArrayList<>();
    private List<PlayerData> filtered = new ArrayList<>();
    private int scrollOffset = 0;

    private EditBox searchField;
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private int panelX;
    private int panelY;

    public PlayerSelectorScreen() {
        super(Component.literal("Player Selector"));
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;

        searchField = new EditBox(this.font, panelX + PADDING, panelY + 30, PANEL_WIDTH - PADDING * 2, 18, Component.literal("Search"));
        searchField.setMaxLength(64);
        searchField.setResponder(this::filterPlayers);
        this.addRenderableWidget(searchField);
        this.setInitialFocus(searchField);

        loadOnlinePlayers();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        GuiUtil.drawPanel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.text(this.font, "Player Selector", panelX + 14, panelY + 12, GuiUtil.TEXT_PRIMARY, false);
        graphics.fill(panelX + 14, panelY + 60, panelX + PANEL_WIDTH - 14, panelY + 61, GuiUtil.PANEL_BORDER);
        if (filtered.isEmpty()) {
            graphics.text(this.font, "No players found", panelX + 14, panelY + 70, GuiUtil.TEXT_MUTED, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean enter = event.key() == 257 || event.key() == 335; // ENTER / KP_ENTER
        if (enter && searchField.isFocused() && !searchField.getValue().isBlank()) {
            openModerationScreen(searchField.getValue().trim(), null);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxOffset = Math.max(0, filtered.size() - VISIBLE_ROWS);
        scrollOffset = Math.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxOffset);
        rebuildVisibleRows();
        return true;
    }

    private void loadOnlinePlayers() {
        allPlayers.clear();
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                allPlayers.add(new PlayerData(info.getProfile().name(), info.getProfile().id()));
            }
        }
        filterPlayers(searchField == null ? "" : searchField.getValue());
    }

    private void filterPlayers(String query) {
        String lower = query.toLowerCase(Locale.ROOT).trim();
        filtered = lower.isEmpty()
                ? new ArrayList<>(allPlayers)
                : allPlayers.stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(lower)).toList();
        scrollOffset = 0;
        rebuildVisibleRows();
    }

    private void rebuildVisibleRows() {
        rowWidgets.forEach(this::removeWidget);
        rowWidgets.clear();

        int listTop = panelY + 70;
        int end = Math.min(filtered.size(), scrollOffset + VISIBLE_ROWS);
        for (int i = scrollOffset; i < end; i++) {
            PlayerData data = filtered.get(i);
            int rowY = listTop + (i - scrollOffset) * (ROW_HEIGHT + ROW_GAP);
            PlayerRow row = new PlayerRow(panelX + PADDING, rowY, PANEL_WIDTH - PADDING * 2, ROW_HEIGHT, data);
            rowWidgets.add(row);
            this.addRenderableWidget(row);
        }
    }

    private void openModerationScreen(String name, UUID uuid) {
        Minecraft.getInstance().gui.setScreen(new ModerationScreen(name, uuid));
    }

    private class PlayerRow extends AbstractWidget {
        private final PlayerData data;

        PlayerRow(int x, int y, int w, int h, PlayerData data) {
            super(x, y, w, h, Component.literal(data.name()));
            this.data = data;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            GuiUtil.drawButton(graphics, getX(), getY(), width, height, GuiUtil.SURFACE, isHovered(), true, false);

            Identifier skin = PlayerSkins.resolve(data.uuid());
            int iconSize = height - 6;
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, getX() + 3, getY() + 3, 8, 8, iconSize, iconSize, 8, 8, 64, 64);
            graphics.text(font, data.name(), getX() + 3 + iconSize + 8, getY() + (height - font.lineHeight) / 2, GuiUtil.TEXT_PRIMARY, false);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            openModerationScreen(data.name(), data.uuid());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
            builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
        }
    }
}
