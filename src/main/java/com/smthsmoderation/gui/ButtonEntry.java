package com.smthsmoderation.gui;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ButtonEntry extends AbstractConfigListEntry<Void> {

    private final Runnable onClick;
    private final int textColor;
    private int lastX, lastY, lastW, lastH;

    public ButtonEntry(Text label, int textColor, Runnable onClick) {
        super(label, false);
        this.textColor = textColor;
        this.onClick = onClick;
    }

    @Override
    public Void getValue() { return null; }

    @Override
    public Optional<Void> getDefaultValue() { return Optional.empty(); }

    @Override
    public void save() {}

    @Override
    public boolean isEdited() { return false; }

    @Override
    public int getItemHeight() { return 22; }

    @Override
    public int getInitialReferenceOffset() { return 0; }

    @Override
    public List<? extends Selectable> narratables() { return List.of(); }

    @Override
    public List<? extends Element> children() { return List.of(); }

    @Override
    public Iterator<String> getSearchTags() { return Collections.emptyIterator(); }

    @Override
    public boolean mouseClicked(Click click, boolean isDouble) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (mx >= lastX && mx <= lastX + lastW && my >= lastY && my <= lastY + lastH) {
            var p = MinecraftClient.getInstance().player;
            if (p != null) p.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    public void render(DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        lastX = x; lastY = y; lastW = entryWidth; lastH = entryHeight;
        boolean hover = isMouseInside(x, y, entryWidth, entryHeight, mouseX, mouseY);
        ctx.fill(x + 1, y + 1, x + entryWidth - 1, y + entryHeight - 1, hover ? 0xFF505050 : 0xFF3C3C3C);
        ctx.fill(x, y, x + entryWidth, y + 1, hover ? 0xFF777777 : 0xFF555555);
        ctx.fill(x, y + 1, x + 1, y + entryHeight - 1, hover ? 0xFF777777 : 0xFF555555);
        ctx.fill(x, y + entryHeight - 1, x + entryWidth, y + entryHeight, hover ? 0xFF222222 : 0xFF111111);
        ctx.fill(x + entryWidth - 1, y + 1, x + entryWidth, y + entryHeight - 1, hover ? 0xFF222222 : 0xFF111111);
        ctx.fill(x + 1, y + entryHeight, x + entryWidth + 1, y + entryHeight + 1, 0xAA000000);
        ctx.fill(x + entryWidth, y + 1, x + entryWidth + 1, y + entryHeight + 1, 0xAA000000);
        var tr = MinecraftClient.getInstance().textRenderer;
        Text text = getFieldName();
        int color = hover ? 0xFFFFFFFF : textColor;
        ctx.drawText(tr, text, x + entryWidth / 2 - tr.getWidth(text) / 2, y + (entryHeight - tr.fontHeight) / 2, color, false);
    }
}
