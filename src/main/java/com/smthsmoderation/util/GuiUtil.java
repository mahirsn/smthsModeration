package com.smthsmoderation.util;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.Color;

public class GuiUtil {

    public static ButtonComponent.Renderer modernButton(int baseColor, int hoverColor, int disabledColor) {
        int shadow = darken(baseColor, 0.4);
        int hoverShadow = darken(hoverColor, 0.4);
        int disabledShadow = darken(disabledColor, 0.4);
        return modernRenderer(baseColor, shadow, hoverColor, hoverShadow, disabledColor, disabledShadow);
    }

    private static ButtonComponent.Renderer modernRenderer(int base, int baseShadow,
                                                            int hover, int hoverShadow,
                                                            int disabled, int disabledShadow) {
        return (context, button, delta) -> {
            int x = button.getX();
            int y = button.getY();
            int w = button.getWidth();
            int h = button.getHeight();

            if (button.active) {
                int fillColor, shadowColor;
                if (button.isHovered()) {
                    fillColor = hover;
                    shadowColor = hoverShadow;
                } else {
                    fillColor = base;
                    shadowColor = baseShadow;
                }
                context.fill(x + 1, y + h, x + w + 2, y + h + 2, shadowColor);
                context.fill(x + w, y + 1, x + w + 2, y + h, shadowColor);
                context.fill(x, y, x + w, y + h, fillColor);
            } else {
                context.fill(x + 1, y + h, x + w + 2, y + h + 2, disabledShadow);
                context.fill(x + w, y + 1, x + w + 2, y + h, disabledShadow);
                context.fill(x, y, x + w, y + h, disabled);
            }
        };
    }

    private static int darken(int argb, double factor) {
        int a = (argb >> 24) & 0xFF;
        int r = (int)(((argb >> 16) & 0xFF) * factor);
        int g = (int)(((argb >> 8) & 0xFF) * factor);
        int b = (int)((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static final int RED = 0xFF8B0000;
    public static final int RED_HOVER = 0xFFCC0000;
    public static final int GREEN = 0xFF006400;
    public static final int GREEN_HOVER = 0xFF008800;
    public static final int YELLOW = 0xFF8B8B00;
    public static final int YELLOW_HOVER = 0xFFCCCC00;
    public static final int DISABLED = 0xFF666666;
    public static final int DARK_BG = 0xFF333333;
    public static final int DARK_BG_HOVER = 0xFF444444;
    public static final int PANEL_BG = 0xCC202020;

    public static ButtonComponent.Renderer outlinedButton(int baseColor, int hoverColor, int disabledColor, int outlineColor) {
        int baseShadow = darken(baseColor, 0.4);
        int hShadow = darken(hoverColor, 0.4);
        int dShadow = darken(disabledColor, 0.4);
        return (context, button, delta) -> {
            int x = button.getX();
            int y = button.getY();
            int w = button.getWidth();
            int h = button.getHeight();
            if (button.active) {
                int fillColor, shadowColor;
                if (button.isHovered()) {
                    fillColor = hoverColor;
                    shadowColor = hShadow;
                } else {
                    fillColor = baseColor;
                    shadowColor = baseShadow;
                }
                context.fill(x - 1, y - 1, x + w + 1, y + h + 1, outlineColor);
                context.fill(x + 1, y + h, x + w + 2, y + h + 2, shadowColor);
                context.fill(x + w, y + 1, x + w + 2, y + h, shadowColor);
                context.fill(x, y, x + w, y + h, fillColor);
            } else {
                context.fill(x + 1, y + h, x + w + 2, y + h + 2, dShadow);
                context.fill(x + w, y + 1, x + w + 2, y + h, dShadow);
                context.fill(x, y, x + w, y + h, disabledColor);
            }
        };
    }
}
