package com.smthsmoderation.gui;

import com.smthsmoderation.util.GuiUtil;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerSelectorScreen extends BaseOwoScreen<FlowLayout> {

    private TextBoxComponent searchField;
    private FlowLayout playerList;
    private List<PlayerData> allPlayers = new ArrayList<>();

    private record PlayerData(String name, Identifier skin) {}

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT);
        root.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fixed(260), Sizing.fixed(320));
        panel.surface(Surface.flat(GuiUtil.PANEL_BG));
        panel.padding(Insets.of(12));
        panel.gap(6);

        panel.child(UIComponents.label(Text.literal("§lPlayer Selector"))
            .sizing(Sizing.fill(), Sizing.content()));

        searchField = UIComponents.textBox(Sizing.fill(), "");
        searchField.setMaxLength(64);
        searchField.onChanged().subscribe(text -> filterPlayers(text));
        searchField.keyPress().subscribe(input -> {
            if (input.key() == 257 && !searchField.getText().trim().isEmpty()) {
                openModerationScreen(searchField.getText().trim(), null);
                return true;
            }
            return false;
        });
        panel.child(searchField);

        panel.child(UIComponents.label(Text.literal("§7Online Players:")));

        ScrollContainer<FlowLayout> scroll = UIContainers.verticalScroll(
            Sizing.fill(), Sizing.fill(),
            UIContainers.verticalFlow(Sizing.fill(), Sizing.content())
        );
        playerList = scroll.child();
        playerList.gap(2);

        loadOnlinePlayers();
        panel.child(scroll);
        root.child(panel);
    }

    private void loadOnlinePlayers() {
        allPlayers.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;

        for (PlayerListEntry entry : client.getNetworkHandler().getListedPlayerListEntries()) {
            String name = entry.getProfile().name();
            Identifier skin = entry.getSkinTextures().body().texturePath();
            allPlayers.add(new PlayerData(name, skin));
        }
        rebuildPlayerList(allPlayers);
    }

    private void filterPlayers(String query) {
        if (query.isEmpty()) {
            rebuildPlayerList(allPlayers);
            return;
        }
        String lower = query.toLowerCase();
        rebuildPlayerList(allPlayers.stream()
            .filter(p -> p.name().toLowerCase().contains(lower))
            .collect(Collectors.toList()));
    }

    private void rebuildPlayerList(List<PlayerData> players) {
        playerList.clearChildren();
        for (PlayerData data : players) {
            addPlayerRow(data.name(), data.skin());
        }
    }

    private void addPlayerRow(String name, Identifier skinTexture) {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(22));
        row.gap(6);
        row.padding(Insets.of(2));

        row.child(UIComponents.texture(skinTexture, 8, 8, 8, 8, 64, 64)
            .sizing(Sizing.fixed(18), Sizing.fixed(18)));

        row.child(UIComponents.label(Text.literal(name))
            .sizing(Sizing.fill(), Sizing.content()));

        row.mouseDown().subscribe((click, doubled) -> {
            openModerationScreen(name, skinTexture);
            return true;
        });

        playerList.child(row);
    }

    private void openModerationScreen(String name, Identifier skinTexture) {
        if (skinTexture == null) {
            skinTexture = Identifier.ofVanilla("textures/entity/steve.png");
        }
        MinecraftClient.getInstance().setScreen(new ModerationScreen(name, skinTexture));
    }
}
