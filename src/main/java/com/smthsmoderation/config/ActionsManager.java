package com.smthsmoderation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.smthsmoderation.gui.SmthsConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ActionsManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ACTIONS_PATH = Path.of("config", "smthsmoderation_actions.json");
    private static final Path CONFIG_PATH = Path.of("config", "smthsmoderation_config.json");
    public static final Type LIST_TYPE = new TypeToken<List<ModerationAction>>(){}.getType();
    public static final Type CONFIG_TYPE = new TypeToken<ModConfig>(){}.getType();

    public static List<ModerationAction> actions = new ArrayList<>();
    public static boolean modEnabled = true;
    public static boolean enableEntityClick = true;
    public static boolean enableChatClick = true;
    public static boolean showHistoryButton = true;
    public static int historyCommandLimit = 10;

    public static class ModConfig {
        public boolean modEnabled = true;
        public boolean enableEntityClick = true;
        public boolean enableChatClick = true;
        public boolean showHistoryButton = true;
        public int historyCommandLimit = 10;
    }

    public static void load() {
        loadConfig();
        try {
            if (Files.exists(ACTIONS_PATH)) {
                String json = Files.readString(ACTIONS_PATH);
                List<ModerationAction> loaded = GSON.fromJson(json, LIST_TYPE);
                if (loaded != null && !loaded.isEmpty()) {
                    actions = loaded;
                    return;
                }
            }
        } catch (Exception ignored) {}
        actions = createDefaults();
        save();
    }

    private static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                ModConfig cfg = GSON.fromJson(json, CONFIG_TYPE);
                if (cfg != null) {
                    modEnabled = cfg.modEnabled;
                    enableEntityClick = cfg.enableEntityClick;
                    enableChatClick = cfg.enableChatClick;
                    showHistoryButton = cfg.showHistoryButton;
                    historyCommandLimit = cfg.historyCommandLimit;
                    return;
                }
            }
        } catch (Exception ignored) {}
        modEnabled = true;
        enableEntityClick = true;
        enableChatClick = true;
        showHistoryButton = true;
        historyCommandLimit = 10;
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            ModConfig cfg = new ModConfig();
            cfg.modEnabled = modEnabled;
            cfg.enableEntityClick = enableEntityClick;
            cfg.enableChatClick = enableChatClick;
            cfg.showHistoryButton = showHistoryButton;
            cfg.historyCommandLimit = historyCommandLimit;
            Files.writeString(CONFIG_PATH, GSON.toJson(cfg));
        } catch (IOException ignored) {}
    }

    private static List<ModerationAction> createDefaults() {
        List<ModerationAction> list = new ArrayList<>();

        ModerationAction ban = new ModerationAction("ban", "/ban %player% %duration% %reason%", 0xFFFF5555, "Oyuncuyu sunucudan uzaklaştırır.");
        ban.requiresConfirmation = true;
        ban.smartMultiplierEnabled = true;
        ban.multiplierKeyword = "yasakladı";
        ban.reductionKeyword = "";
        ban.variables.add(new CommandVariable("reason", "hile, uygunsuz icerik, kural ihlali", false));
        ban.variables.add(new CommandVariable("duration", "1d, 7d, 30d", false));
        list.add(ban);

        ModerationAction ipban = new ModerationAction("ipban", "/ipban %player% %duration% %reason%", 0xFFFF5555, "Oyuncunun IP adresini yasaklar.");
        ipban.requiresConfirmation = true;
        ipban.smartMultiplierEnabled = true;
        ipban.multiplierKeyword = "yasakladı";
        ipban.variables.add(new CommandVariable("reason", "hile, proxy/vpn, kural ihlali", false));
        ipban.variables.add(new CommandVariable("duration", "1h, 3h, 6h, 1d, 7d, 30d", false));
        list.add(ipban);

        ModerationAction mute = new ModerationAction("mute", "/mute %player% %duration% %reason%", 0xFF555555, "Oyuncunun sohbet erişimini kapatır.");
        mute.smartMultiplierEnabled = true;
        mute.multiplierKeyword = "susturdu";
        mute.reductionKeyword = "";
        mute.variables.add(new CommandVariable("reason", "spam, küfür, argo, hakaret", false));
        mute.variables.add(new CommandVariable("duration", "30m, 1h, 6h, 1d, 7d, 30d", false));
        list.add(mute);

        ModerationAction voiceMute = new ModerationAction("voice mute", "/lp user %player% permission settemp voicechat.speak false %duration%", 0xFF555555, "Oyuncunun sesli sohbet yetkisini süreli olarak alır.");
        voiceMute.variables.add(new CommandVariable("duration", "1h, 3h, 6h, 1d, 7d, 30d", false));
        list.add(voiceMute);

        list.add(new ModerationAction("tp", "/tp %player%", 0xFF555555, "Hedef oyuncunun yanına ışınlanır."));
        list.add(new ModerationAction("tphere", "/tphere %player%", 0xFF555555, "Hedef oyuncuyu yanına çeker."));
        list.add(new ModerationAction("invsee", "/invsee %player%", 0xFF555555, "Oyuncunun envanterini açar."));
        list.add(new ModerationAction("axir view", "/axir view %player%", 0xFF555555, "Oyuncunun ceza geçmişini görüntüler."));

        return list;
    }

    public static void save() {
        try {
            Files.createDirectories(ACTIONS_PATH.getParent());
            Files.writeString(ACTIONS_PATH, GSON.toJson(actions));
        } catch (IOException ignored) {}
    }

    public static Screen buildScreen(Screen parent) {
        return SmthsConfigScreen.build(parent);
    }
}
