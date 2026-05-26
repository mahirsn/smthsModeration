package com.smthsmoderation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

public class ModerationAction {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String type = "warn";
    public String commandTemplate = "/" + type + " %player% %reason%";
    public int buttonColor = 0xFF8B0000;
    public String description = "";
    public boolean requiresReason = false;
    public boolean requiresDuration = false;
    public boolean requiresConfirmation = false;
    public boolean isVisible = true;
    public List<CommandVariable> variables = new ArrayList<>();

    // Smart Penalty Multiplier fields
    public boolean smartMultiplierEnabled = false;
    public String multiplierKeyword = "";
    public String basePenaltyTime = "30m";
    public double multiplierStep = 0.20;
    public double multiplierMax = 3.0;

    public ModerationAction() {}

    public ModerationAction(String type, String template, int color, String desc) {
        this.type = type;
        this.commandTemplate = template;
        this.buttonColor = color;
        this.description = desc;
        this.requiresReason = template.contains("%reason%");
        this.requiresDuration = template.contains("%duration%");
    }

    public String serialize() {
        return GSON.toJson(this);
    }

    public static ModerationAction deserialize(String s) {
        try {
            return GSON.fromJson(s, ModerationAction.class);
        } catch (Exception e) {
            return new ModerationAction();
        }
    }

    public String summary() {
        return type.toUpperCase();
    }

    private static final int DEFAULT_COLOR = 0xFFAAAAAA;

    public int getColor() {
        return buttonColor != 0 ? buttonColor : DEFAULT_COLOR;
    }

    public int getHoverColor() {
        return lighten(buttonColor, 0.3);
    }

    private static int lighten(int argb, double factor) {
        int a = argb & 0xFF000000;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        r = (int)(r + (255 - r) * factor);
        g = (int)(g + (255 - g) * factor);
        b = (int)(b + (255 - b) * factor);
        return a | (r << 16) | (g << 8) | b;
    }
}
