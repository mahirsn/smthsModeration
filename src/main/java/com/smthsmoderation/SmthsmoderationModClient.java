package com.smthsmoderation;

import com.mojang.blaze3d.platform.InputConstants;
import com.smthsmoderation.chat.ChatBacklog;
import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.gui.ModerationScreen;
import com.smthsmoderation.gui.PlayerSelectorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * Sole entrypoint. Registered only as "client" in fabric.mod.json — this mod
 * has no server-side behavior, so there is no "main" entrypoint doing
 * nothing (a prior version shipped one anyway).
 */
public class SmthsmoderationModClient implements ClientModInitializer {

    private static final String KEY_TRANSLATION = "key.smthsmoderation.open";

    @Override
    public void onInitializeClient() {
        ActionsManager.load();
        registerKeybind();
        registerShiftClickHandler();
        registerCommands();
        registerChatListeners();
    }

    private void registerKeybind() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("smthsmoderation", "general"));
        KeyMapping openKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(KEY_TRANSLATION, InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (ActionsManager.config.modEnabled) {
                    client.gui.setScreen(new PlayerSelectorScreen());
                }
            }
        });
    }

    private void registerShiftClickHandler() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            boolean eligible = ActionsManager.config.modEnabled && ActionsManager.config.enableEntityClick
                    && level.isClientSide() && player.isShiftKeyDown() && entity instanceof Player;
            if (!eligible) return InteractionResult.PASS;

            Player target = (Player) entity;
            String name = target.getGameProfile().name();
            Minecraft.getInstance().gui.setScreen(new ModerationScreen(name, target.getUUID()));
            return InteractionResult.SUCCESS;
        });
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("smthsmoderation").executes(this::openPlayerSelector));
            dispatcher.register(ClientCommands.literal("moderate").executes(this::openPlayerSelector));
        });
    }

    private int openPlayerSelector(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context) {
        if (!ActionsManager.config.modEnabled) return 0;
        Minecraft.getInstance().gui.setScreen(new PlayerSelectorScreen());
        return 1;
    }

    private void registerChatListeners() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();
            ChatBacklog.push(text);
            if (text.trim().startsWith("➩")) {
                ModerationScreen.appendHistoryLine(text);
            }
        });
    }
}
