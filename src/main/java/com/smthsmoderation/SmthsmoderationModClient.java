package com.smthsmoderation;

import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.gui.ModerationScreen;
import com.smthsmoderation.gui.PlayerSelectorScreen;
import com.smthsmoderation.util.ChatBacklog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import static net.minecraft.util.Identifier.of;
import org.lwjgl.glfw.GLFW;

public class SmthsmoderationModClient implements ClientModInitializer {

    private static final String KEY_OPEN = "key.smthsmoderation.open";

    @Override
    public void onInitializeClient() {
        ActionsManager.load();
        registerKeybind();
        registerShiftClickHandler();
        registerCommands();
        registerHistoryInterceptor();
        registerChatBacklog();
    }

    private void registerKeybind() {
        KeyBinding openKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(KEY_OPEN, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, new KeyBinding.Category(of("smthsmoderation", "general")))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ActionsManager.modEnabled && openKey.wasPressed()) {
                client.setScreen(new PlayerSelectorScreen());
            }
        });
    }

    private void registerShiftClickHandler() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!ActionsManager.modEnabled || !ActionsManager.enableEntityClick || !world.isClient() || !player.isSneaking() || !(entity instanceof PlayerEntity target)) {
                return ActionResult.PASS;
            }

            String name = target.getGameProfile().name();
            Identifier skin = Identifier.ofVanilla("textures/entity/steve.png");

            var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
            if (networkHandler != null) {
                var entry = networkHandler.getPlayerListEntry(target.getUuid());
                if (entry != null) {
                    skin = entry.getSkinTextures().body().texturePath();
                }
            }

            MinecraftClient.getInstance().setScreen(new ModerationScreen(name, skin));
            return ActionResult.SUCCESS;
        });
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("smthsmoderation")
                .executes(context -> {
                    if (!ActionsManager.modEnabled) return 0;
                    MinecraftClient.getInstance().setScreen(new PlayerSelectorScreen());
                    return 1;
                })
            );

            dispatcher.register(ClientCommandManager.literal("moderate")
                .executes(context -> {
                    if (!ActionsManager.modEnabled) return 0;
                    MinecraftClient.getInstance().setScreen(new PlayerSelectorScreen());
                    return 1;
                })
            );
        });
    }

    private void registerHistoryInterceptor() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && !message.getString().trim().startsWith("➩")) return;
            ModerationScreen.appendHistoryLine(message);
        });
    }

    private void registerChatBacklog() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            ChatBacklog.push(message.getString());
        });
    }
}
