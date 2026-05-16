package com.smthsmoderation;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmthsmoderationMod implements ModInitializer {
    public static final String MOD_ID = "smthsmoderation";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Smthsmoderation mod initialized!");
    }
}
