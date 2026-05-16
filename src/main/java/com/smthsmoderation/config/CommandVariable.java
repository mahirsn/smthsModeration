package com.smthsmoderation.config;

import java.util.ArrayList;
import java.util.List;

public class CommandVariable {
    public String name = "";
    public String presets = "";
    public boolean isRequired = false;

    public CommandVariable() {}

    public CommandVariable(String name, String presets, boolean isRequired) {
        this.name = name;
        this.presets = presets;
        this.isRequired = isRequired;
    }

    public List<String> getPresetList() {
        List<String> list = new ArrayList<>();
        if (presets == null || presets.isBlank()) return list;
        for (String p : presets.split(",")) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        return list;
    }
}
