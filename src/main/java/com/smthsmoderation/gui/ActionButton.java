package com.smthsmoderation.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Flat colored button used for actions, chips, and player-list rows.
 * Vanilla's {@code Button} draws its own 9-slice texture and doesn't offer
 * a clean hook to replace that look, so this is a direct
 * {@link AbstractWidget} instead of a Button subclass.
 */
public class ActionButton extends AbstractWidget {

    private final Font font;
    private final int baseColor;
    private final Runnable onPress;
    private boolean selected;

    public ActionButton(Font font, int x, int y, int width, int height, Component label,
                         int baseColor, Runnable onPress) {
        super(x, y, width, height, label);
        this.font = font;
        this.baseColor = baseColor;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        GuiUtil.drawButton(graphics, getX(), getY(), width, height, baseColor, isHovered(), active, selected);

        int textColor = active ? GuiUtil.TEXT_PRIMARY : GuiUtil.TEXT_MUTED;
        int textX = getX() + (width - font.width(getMessage())) / 2;
        int textY = getY() + (height - font.lineHeight) / 2;
        graphics.text(font, getMessage(), textX, textY, textColor, false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}
