package com.smthsmoderation.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Resolves a player's skin texture from the online player list, falling back to Steve. */
final class PlayerSkins {

    static final Identifier STEVE = Identifier.withDefaultNamespace("textures/entity/steve.png");

    private PlayerSkins() {
    }

    static Identifier resolve(UUID uuid) {
        if (uuid == null) return STEVE;
        try {
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) return STEVE;
            PlayerInfo info = connection.getPlayerInfo(uuid);
            if (info == null) return STEVE;
            return info.getSkin().body().texturePath();
        } catch (Exception e) {
            return STEVE;
        }
    }
}
