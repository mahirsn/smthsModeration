package com.smthsmoderation.gui;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** A full-width clickable row in the Cloth Config list, e.g. "[+] Add New Action". */
public class ButtonEntry extends AbstractConfigListEntry<Void> {

    private final Runnable onClick;
    private final int textColor;
    private int lastX, lastY, lastW, lastH;

    public ButtonEntry(Component label, int textColor, Runnable onClick) {
        super(label, false);
        this.textColor = textColor;
        this.onClick = onClick;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public void save() {
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public int getItemHeight() {
        return 22;
    }

    @Override
    public int getInitialReferenceOffset() {
        return 0;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of();
    }

    @Override
    public Iterator<String> getSearchTags() {
        return Collections.emptyIterator();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        int mx = (int) event.x();
        int my = (int) event.y();
        if (mx >= lastX && mx <= lastX + lastW && my >= lastY && my <= lastY + lastH) {
            var player = Minecraft.getInstance().player;
            if (player != null) player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth,
                                    int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        lastX = x;
        lastY = y;
        lastW = entryWidth;
        lastH = entryHeight;
        boolean hover = isMouseInside(mouseX, mouseY, x, y, entryWidth, entryHeight);
        int margin = 2;
        GuiUtil.drawButton(graphics, x, y + margin, entryWidth, entryHeight - margin * 2, GuiUtil.SURFACE, hover, true, false);

        var font = Minecraft.getInstance().font;
        Component text = getFieldName();
        graphics.text(font, text, x + entryWidth / 2 - font.width(text) / 2, y + (entryHeight - font.lineHeight) / 2, textColor, false);
    }
}
