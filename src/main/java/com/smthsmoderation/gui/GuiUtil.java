package com.smthsmoderation.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared flat, minimal rendering style used across this mod's screens:
 * solid fills, a thin 1px border, hover feedback via lightening, and a
 * single accent color for selection/focus. No drop shadows or 3D bevels.
 */
public final class GuiUtil {

    /** Neutral chrome palette (panels, secondary buttons) — not the user-configurable action colors. */
    public static final int PANEL_BG = 0xE6181818;
    public static final int PANEL_BORDER = 0x26FFFFFF;
    public static final int SURFACE = 0xFF242424;
    public static final int SURFACE_HOVER = 0xFF2E2E2E;
    public static final int ACCENT = 0xFF5B8DEF;
    public static final int TEXT_PRIMARY = 0xFFEDEDED;
    public static final int TEXT_MUTED = 0xFF9A9A9A;
    public static final int DISABLED = 0xFF303030;

    private static final int BORDER = 0x40000000;

    private GuiUtil() {
    }

    /** Flat panel background with a subtle 1px border. */
    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_BG);
        drawBorder(graphics, x, y, w, h, PANEL_BORDER);
    }

    /** Flat button/row: solid fill, thin border, optional accent ring when selected. */
    public static void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                   int baseColor, boolean hovered, boolean active, boolean selected) {
        int fill = !active ? DISABLED : (hovered ? lighten(baseColor, 0.18) : baseColor);
        graphics.fill(x, y, x + w, y + h, fill);
        drawBorder(graphics, x, y, w, h, selected ? ACCENT : BORDER);
        if (selected) {
            drawBorder(graphics, x - 1, y - 1, w + 2, h + 2, ACCENT);
        }
    }

    private static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static int lighten(int argb, double factor) {
        int a = argb & 0xFF000000;
        int r = (int) (((argb >> 16) & 0xFF) * (1 - factor) + 255 * factor);
        int g = (int) (((argb >> 8) & 0xFF) * (1 - factor) + 255 * factor);
        int b = (int) ((argb & 0xFF) * (1 - factor) + 255 * factor);
        return a | (r << 16) | (g << 8) | b;
    }
}
