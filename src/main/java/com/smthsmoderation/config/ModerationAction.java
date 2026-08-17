package com.smthsmoderation.config;

import java.util.ArrayList;
import java.util.List;

/** One moderation button: a command template plus its UI and penalty-multiplier settings. */
public class ModerationAction {

    private static final int DEFAULT_COLOR = 0xFFAAAAAA;

    public String type = "warn";
    public String commandTemplate = "/warn %player% %reason%";
    public int buttonColor = 0xFF8B0000;
    public String description = "";
    public boolean requiresReason = false;
    public boolean requiresDuration = false;
    public boolean requiresConfirmation = false;
    public boolean isVisible = true;
    public List<CommandVariable> variables = new ArrayList<>();

    public boolean smartMultiplierEnabled = false;
    public String multiplierKeyword = "";
    public String basePenaltyTime = "30m";
    public double multiplierStep = 0.20;
    public double multiplierMax = 3.0;
    public String reductionKeyword = "";
    public String targetVariableForPM = "duration";

    public boolean sendWebhook = true;

    public ModerationAction() {
    }

    public ModerationAction(String type, String commandTemplate, int buttonColor, String description) {
        this.type = type;
        this.commandTemplate = commandTemplate;
        this.buttonColor = buttonColor;
        this.description = description;
        this.requiresReason = commandTemplate.contains("%reason%");
        this.requiresDuration = commandTemplate.contains("%duration%");
    }

    public int getColor() {
        return buttonColor != 0 ? buttonColor : DEFAULT_COLOR;
    }

    public int getHoverColor() {
        return lighten(getColor(), 0.3);
    }

    private static int lighten(int argb, double factor) {
        int a = argb & 0xFF000000;
        int r = (int) (((argb >> 16) & 0xFF) * (1 - factor) + 255 * factor);
        int g = (int) (((argb >> 8) & 0xFF) * (1 - factor) + 255 * factor);
        int b = (int) ((argb & 0xFF) * (1 - factor) + 255 * factor);
        return a | (r << 16) | (g << 8) | b;
    }
}
