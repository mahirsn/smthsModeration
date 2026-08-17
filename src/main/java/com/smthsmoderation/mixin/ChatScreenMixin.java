package com.smthsmoderation.mixin;

import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.gui.ModerationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opens {@link ModerationScreen} on Shift+click of a player name in chat.
 * Player name is read from the clicked component's hover text first, then
 * from its click-command arguments. Target signature verified against the
 * real 26.2 Minecraft jar via javap: {@code ChatScreen} declares a private
 * {@code handleComponentClicked(Style, boolean)} — Mixin can inject into
 * private methods, so the reduced visibility doesn't matter here.
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void onComponentClicked(Style style, boolean unused, CallbackInfoReturnable<Boolean> cir) {
        boolean eligible = ActionsManager.config.modEnabled && ActionsManager.config.enableChatClick && isShiftDown();
        if (!eligible || style == null) return;

        String name = extractPlayerName(style);
        if (name != null) {
            Minecraft.getInstance().gui.setScreen(new ModerationScreen(name, null));
            cir.setReturnValue(true);
        }
    }

    private boolean isShiftDown() {
        long handle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private String extractPlayerName(Style style) {
        if (style.getHoverEvent() instanceof HoverEvent.ShowText showText) {
            String text = showText.value().getString().trim();
            if (isValidPlayerName(text)) return text;
        }

        if (style.getClickEvent() instanceof ClickEvent.SuggestCommand suggest) {
            return extractNameFromCommand(suggest.command());
        }
        if (style.getClickEvent() instanceof ClickEvent.RunCommand run) {
            return extractNameFromCommand(run.command());
        }
        return null;
    }

    private String extractNameFromCommand(String command) {
        if (command == null || command.isEmpty()) return null;
        for (String part : command.split(" ")) {
            if (isValidPlayerName(part)) return part;
        }
        return null;
    }

    private boolean isValidPlayerName(String name) {
        return name != null && name.length() >= 3 && name.length() <= 16 && name.matches("[a-zA-Z0-9_]+");
    }
}
