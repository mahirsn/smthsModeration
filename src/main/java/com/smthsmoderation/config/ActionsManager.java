package com.smthsmoderation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and persists the mod's two config files. Holds the live settings as
 * static state (this mod only ever runs as one client instance per JVM) but
 * does no business logic itself — see the {@code action} package for that.
 */
public final class ActionsManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ACTIONS_PATH = Path.of("config", "smthsmoderation_actions.json");
    private static final Path CONFIG_PATH = Path.of("config", "smthsmoderation_config.json");
    private static final Type ACTIONS_LIST_TYPE = new TypeToken<List<ModerationAction>>() {}.getType();

    public static List<ModerationAction> actions = new ArrayList<>();
    public static ModConfig config = new ModConfig();

    private ActionsManager() {
    }

    public static void load() {
        config = readJson(CONFIG_PATH, ModConfig.class, new ModConfig());

        List<ModerationAction> loaded = readJson(ACTIONS_PATH, ACTIONS_LIST_TYPE, null);
        actions = (loaded != null && !loaded.isEmpty()) ? loaded : createDefaults();
        if (loaded == null || loaded.isEmpty()) save();
    }

    public static void save() {
        writeJson(ACTIONS_PATH, actions);
    }

    public static void saveConfig() {
        writeJson(CONFIG_PATH, config);
    }

    private static <T> T readJson(Path path, Type type, T fallback) {
        try {
            if (Files.exists(path)) {
                T value = GSON.fromJson(Files.readString(path), type);
                if (value != null) return value;
            }
        } catch (Exception ignored) {
            // Missing or malformed config falls back to defaults.
        }
        return fallback;
    }

    private static void writeJson(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(value));
        } catch (IOException ignored) {
            // Best-effort persistence; the in-memory state stays authoritative either way.
        }
    }

    private static List<ModerationAction> createDefaults() {
        List<ModerationAction> list = new ArrayList<>();

        ModerationAction ban = new ModerationAction("ban", "/ban %player% %duration% %reason%", 0xFFFF5555,
                "Oyuncuyu sunucudan uzaklaştırır.");
        ban.requiresConfirmation = true;
        ban.smartMultiplierEnabled = true;
        ban.multiplierKeyword = "yasakladı";
        ban.variables.add(new CommandVariable("reason", "hile, uygunsuz icerik, kural ihlali", false));
        ban.variables.add(new CommandVariable("duration", "1d, 7d, 30d", false));
        list.add(ban);

        ModerationAction ipban = new ModerationAction("ipban", "/ipban %player% %duration% %reason%", 0xFFFF5555,
                "Oyuncunun IP adresini yasaklar.");
        ipban.requiresConfirmation = true;
        ipban.smartMultiplierEnabled = true;
        ipban.multiplierKeyword = "yasakladı";
        ipban.variables.add(new CommandVariable("reason", "hile, proxy/vpn, kural ihlali", false));
        ipban.variables.add(new CommandVariable("duration", "1h, 3h, 6h, 1d, 7d, 30d", false));
        list.add(ipban);

        ModerationAction mute = new ModerationAction("mute", "/mute %player% %duration% %reason%", 0xFF555555,
                "Oyuncunun sohbet erişimini kapatır.");
        mute.smartMultiplierEnabled = true;
        mute.multiplierKeyword = "susturdu";
        mute.variables.add(new CommandVariable("reason", "spam, küfür, argo, hakaret", false));
        mute.variables.add(new CommandVariable("duration", "30m, 1h, 6h, 1d, 7d, 30d", false));
        list.add(mute);

        ModerationAction voiceMute = new ModerationAction("voice mute",
                "/lp user %player% permission settemp voicechat.speak false %duration%", 0xFF555555,
                "Oyuncunun sesli sohbet yetkisini süreli olarak alır.");
        voiceMute.variables.add(new CommandVariable("duration", "1h, 3h, 6h, 1d, 7d, 30d", false));
        list.add(voiceMute);

        list.add(new ModerationAction("tp", "/tp %player%", 0xFF555555, "Hedef oyuncunun yanına ışınlanır."));
        list.add(new ModerationAction("tphere", "/tphere %player%", 0xFF555555, "Hedef oyuncuyu yanına çeker."));
        list.add(new ModerationAction("invsee", "/invsee %player%", 0xFF555555, "Oyuncunun envanterini açar."));
        list.add(new ModerationAction("axir view", "/axir view %player%", 0xFF555555, "Oyuncunun ceza geçmişini görüntüler."));

        return list;
    }
}
