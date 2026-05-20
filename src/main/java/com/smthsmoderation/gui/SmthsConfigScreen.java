package com.smthsmoderation.gui;

import com.smthsmoderation.config.ActionsManager;
import com.smthsmoderation.config.CommandVariable;
import com.smthsmoderation.config.ModerationAction;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class SmthsConfigScreen {

    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF7777;
    private static final Map<String, Boolean> expandedStates = new HashMap<>();

    public static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("§bSmthsModeration Config"))
            .transparentBackground()
            .setSavingRunnable(() -> {})
            .setDoesConfirmSave(true);

        ConfigCategory main = builder.getOrCreateCategory(Text.literal("Moderation Actions"));

        main.addEntry(ConfigEntryBuilder.create()
            .startBooleanToggle(Text.literal("§6§lEnable Mod"), ActionsManager.modEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(v -> {
                ActionsManager.modEnabled = v;
                ActionsManager.saveConfig();
            })
            .build());

        main.addEntry(ConfigEntryBuilder.create()
            .startBooleanToggle(Text.literal("§fEnable Shift+Right-Click on Players"), ActionsManager.enableEntityClick)
            .setDefaultValue(true)
            .setSaveConsumer(v -> {
                ActionsManager.enableEntityClick = v;
                ActionsManager.saveConfig();
            })
            .build());

        main.addEntry(ConfigEntryBuilder.create()
            .startBooleanToggle(Text.literal("§fEnable Shift+Left-Click in Chat"), ActionsManager.enableChatClick)
            .setDefaultValue(true)
            .setSaveConsumer(v -> {
                ActionsManager.enableChatClick = v;
                ActionsManager.saveConfig();
            })
            .build());

        main.addEntry(ConfigEntryBuilder.create()
            .startBooleanToggle(Text.literal("§fShow History Button"), ActionsManager.showHistoryButton)
            .setDefaultValue(true)
            .setSaveConsumer(v -> {
                ActionsManager.showHistoryButton = v;
                ActionsManager.saveConfig();
            })
            .build());

        main.addEntry(ConfigEntryBuilder.create()
            .startTextDescription(Text.literal("§7Highly Customizable Command Executer (designed for moderation)"))
            .build());

        main.addEntry(new ButtonEntry(Text.literal("[Save Config]"), GREEN, () -> {
            Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof AbstractConfigScreen acs) {
                acs.saveAll(false);
            }
            ActionsManager.save();
        }));

        main.addEntry(new ButtonEntry(Text.literal("[+] Add New Action"), GREEN, () -> {
            String uniqueName = "New-" + System.nanoTime();
            ActionsManager.actions.add(new ModerationAction(uniqueName, "/command %player%", 0xFF555555, "New action"));
            rebuild(parent);
        }));

        for (ModerationAction action : ActionsManager.actions) {
            var actionSub = ConfigEntryBuilder.create()
                .startSubCategory(Text.literal(action.type));
            actionSub.setExpanded(expandedStates.getOrDefault(action.type, false));

            actionSub.add(ConfigEntryBuilder.create()
                .startStrField(Text.literal("Action Name"), action.type)
                .setDefaultValue("MyAction")
                .setSaveConsumer(v -> {
                    action.type = v;
                    rebuild(parent);
                })
                .build()
            );

            actionSub.add(ConfigEntryBuilder.create()
                .startStrField(Text.literal("Command Template"), action.commandTemplate)
                .setDefaultValue("/" + action.type.toLowerCase() + " %player% %reason%")
                .setSaveConsumer(v -> action.commandTemplate = v)
                .build()
            );

            String hex = String.format("#%06X", 0xFFFFFF & action.buttonColor);
            actionSub.add(ConfigEntryBuilder.create()
                .startStrField(Text.literal("Button Color (hex)"), hex)
                .setDefaultValue("#8B0000")
                .setSaveConsumer(v -> {
                    try {
                        action.buttonColor = 0xFF000000 | Integer.parseInt(v.replace("#", "").trim(), 16);
                    } catch (NumberFormatException ignored) {}
                })
                .build()
            );

            actionSub.add(ConfigEntryBuilder.create()
                .startStrField(Text.literal("Description"), action.description)
                .setDefaultValue("")
                .setSaveConsumer(v -> action.description = v)
                .build()
            );

            actionSub.add(ConfigEntryBuilder.create()
                .startBooleanToggle(Text.literal("Requires Confirmation"), action.requiresConfirmation)
                .setDefaultValue(false)
                .setSaveConsumer(v -> action.requiresConfirmation = v)
                .build()
            );

            for (int i = 0; i < action.variables.size(); i++) {
                CommandVariable var = action.variables.get(i);
                int vi = i;

                var varSub = ConfigEntryBuilder.create()
                    .startSubCategory(Text.literal("Variable: " + var.name));
                varSub.setExpanded(expandedStates.getOrDefault("Variable: " + var.name, false));

                varSub.add(ConfigEntryBuilder.create()
                    .startStrField(Text.literal("Name"), var.name)
                    .setDefaultValue("var")
                    .setSaveConsumer(v -> var.name = v)
                    .build()
                );

                varSub.add(ConfigEntryBuilder.create()
                    .startStrField(Text.literal("Presets (CSV)"), var.presets)
                    .setDefaultValue("")
                    .setSaveConsumer(v -> var.presets = v)
                    .build()
                );

                varSub.add(ConfigEntryBuilder.create()
                    .startBooleanToggle(Text.literal("Required"), var.isRequired)
                    .setDefaultValue(false)
                    .setSaveConsumer(v -> var.isRequired = v)
                    .build()
                );

                varSub.add(new ButtonEntry(Text.literal("[-] Remove Variable"), RED, () -> {
                    action.variables.remove(vi);
                    rebuild(parent);
                }));

                actionSub.add(varSub.build());
            }

            actionSub.add(new DualButtonEntry(
                Text.literal("[+] Add Custom Variable"), GREEN, () -> {
                    String varName = "new_var" + (action.variables.size() + 1);
                    action.variables.add(new CommandVariable(varName, "", false));
                    rebuild(parent);
                },
                Text.literal("[-] Delete Action"), RED, () -> {
                    ActionsManager.actions.remove(action);
                    rebuild(parent);
                }
            ));

            main.addEntry(actionSub.build());
        }

        return builder.build();
    }

    private static void saveExpandedState(Screen screen) {
        if (screen instanceof ClothConfigScreen clothScreen && clothScreen.listWidget != null) {
            expandedStates.clear();
            for (Element entry : clothScreen.listWidget.children()) {
                if (entry instanceof SubCategoryListEntry sub) {
                    expandedStates.put(sub.getCategoryName().getString(), sub.isExpanded());
                    for (Element subEntry : sub.children()) {
                        if (subEntry instanceof SubCategoryListEntry nestedSub) {
                            expandedStates.put(nestedSub.getCategoryName().getString(), nestedSub.isExpanded());
                        }
                    }
                }
            }
        }
    }

    private static void rebuild(Screen parent) {
        Screen current = MinecraftClient.getInstance().currentScreen;
        saveExpandedState(current);
        MinecraftClient.getInstance().setScreen(build(parent));
    }
}
