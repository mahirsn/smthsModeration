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

public class DualButtonEntry extends AbstractConfigListEntry<Void> {

    private static final int GAP = 6;

    private final Text leftLabel, rightLabel;
    private final int leftColor, rightColor;
    private final Runnable leftClick, rightClick;
    private int lastX, lastY, lastW, lastH;
    private boolean hoverLeft, hoverRight;

    public DualButtonEntry(Text leftLabel, int leftColor, Runnable leftClick, Text rightLabel, int rightColor, Runnable rightClick) {
        super(Text.empty(), false);
        this.leftLabel = leftLabel;
        this.leftColor = leftColor;
        this.leftClick = leftClick;
        this.rightLabel = rightLabel;
        this.rightColor = rightColor;
        this.rightClick = rightClick;
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
        if (my < lastY || my > lastY + lastH) return false;
        int cx = lastX + lastW / 2;
        var tr = MinecraftClient.getInstance().textRenderer;
        int leftW = tr.getWidth(leftLabel) + 16;
        int rightW = tr.getWidth(rightLabel) + 16;
        int totalW = leftW + rightW + GAP;
        int startX = cx - totalW / 2;
        int lx = startX;
        int rx = startX + leftW + GAP;
        if (mx >= lx && mx <= lx + leftW) {
            var p = MinecraftClient.getInstance().player;
            if (p != null) p.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
            leftClick.run();
            return true;
        }
        if (mx >= rx && mx <= rx + rightW) {
            var p = MinecraftClient.getInstance().player;
            if (p != null) p.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
            rightClick.run();
            return true;
        }
        return false;
    }

    @Override
    public void render(DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        lastX = x; lastY = y; lastW = entryWidth; lastH = entryHeight;
        int cx = x + entryWidth / 2;
        var tr = MinecraftClient.getInstance().textRenderer;
        int leftW = tr.getWidth(leftLabel) + 16;
        int rightW = tr.getWidth(rightLabel) + 16;
        int totalW = leftW + rightW + GAP;
        int startX = cx - totalW / 2;
        int lx = startX;
        int rx = startX + leftW + GAP;

        hoverLeft = mouseX >= lx && mouseX <= lx + leftW && mouseY >= y && mouseY <= y + entryHeight;
        hoverRight = mouseX >= rx && mouseX <= rx + rightW && mouseY >= y && mouseY <= y + entryHeight;

        draw3DButton(ctx, lx, y, leftW, entryHeight, hoverLeft);
        draw3DButton(ctx, rx, y, rightW, entryHeight, hoverRight);

        int ltc = hoverLeft ? 0xFFFFFFFF : leftColor;
        int rtc = hoverRight ? 0xFFFFFFFF : rightColor;
        ctx.drawText(tr, leftLabel, lx + leftW / 2 - tr.getWidth(leftLabel) / 2, y + (entryHeight - tr.fontHeight) / 2, ltc, false);
        ctx.drawText(tr, rightLabel, rx + rightW / 2 - tr.getWidth(rightLabel) / 2, y + (entryHeight - tr.fontHeight) / 2, rtc, false);
    }

    private void draw3DButton(DrawContext ctx, int bx, int by, int bw, int bh, boolean hover) {
        int base = hover ? 0xFF444444 : 0xFF222222;
        int light = hover ? 0xFF555555 : 0xFF444444;
        int dark = hover ? 0xFF222222 : 0xFF111111;
        ctx.fill(bx, by, bx + bw, by + bh, base);
        ctx.fill(bx, by, bx + bw, by + 1, light);
        ctx.fill(bx, by, bx + 1, by + bh, light);
        ctx.fill(bx, by + bh - 1, bx + bw, by + bh, dark);
        ctx.fill(bx + bw - 1, by, bx + bw, by + bh, dark);
    }
}
