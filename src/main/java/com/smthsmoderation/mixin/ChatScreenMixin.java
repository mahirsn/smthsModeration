package com.smthsmoderation.mixin;

import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.gui.ModerationScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
    private void onHandleClickEvent(Style style, boolean something, CallbackInfoReturnable<Boolean> cir) {
        if (!ActionsManager.modEnabled || !ActionsManager.enableChatClick || !isShiftDown()) return;

        String name = extractPlayerName(style);
        if (name != null && !name.isEmpty()) {
            Identifier skin = Identifier.ofVanilla("textures/entity/steve.png");
            MinecraftClient.getInstance().setScreen(new ModerationScreen(name, skin));
            cir.setReturnValue(true);
        }
    }

    private boolean isShiftDown() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private String extractPlayerName(Style style) {
        if (style.getHoverEvent() instanceof HoverEvent.ShowText showText) {
            String text = showText.value().getString().trim();
            if (isValidPlayerName(text)) return text;
        }

        if (style.getClickEvent() != null) {
            ClickEvent clickEvent = style.getClickEvent();
            if (clickEvent instanceof ClickEvent.SuggestCommand suggest) {
                String cmd = suggest.command();
                String name = extractNameFromCommand(cmd);
                if (name != null) return name;
            } else if (clickEvent instanceof ClickEvent.RunCommand run) {
                String cmd = run.command();
                String name = extractNameFromCommand(cmd);
                if (name != null) return name;
            }
        }

        return null;
    }

    private String extractNameFromCommand(String command) {
        if (command == null || command.isEmpty()) return null;
        String[] parts = command.split(" ");
        for (String part : parts) {
            if (isValidPlayerName(part)) return part;
        }
        return null;
    }

    private boolean isValidPlayerName(String name) {
        return name != null && name.length() >= 3 && name.length() <= 16 && name.matches("[a-zA-Z0-9_]+");
    }
}
