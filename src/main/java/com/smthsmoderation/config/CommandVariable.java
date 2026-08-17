package com.smthsmoderation.config;

import java.util.ArrayList;
import java.util.List;

/** A fillable slot in a {@link ModerationAction}'s command template, e.g. %reason%. */
public class CommandVariable {
    public String name = "";
    public String presets = "";
    public boolean isRequired = false;

    public CommandVariable() {
    }

    public CommandVariable(String name, String presets, boolean isRequired) {
        this.name = name;
        this.presets = presets;
        this.isRequired = isRequired;
    }

    /** Comma-separated {@link #presets}, trimmed, blanks dropped. */
    public List<String> getPresetList() {
        List<String> list = new ArrayList<>();
        if (presets == null || presets.isBlank()) return list;
        for (String preset : presets.split(",")) {
            String trimmed = preset.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        return list;
    }
}
