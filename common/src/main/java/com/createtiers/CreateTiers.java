package com.createtiers;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateTiers {
    public static final String MOD_ID = "createtiers";
    public static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers");

    public static int PACK_FORMAT = 15;
    public static int SERVER_PACK_FORMAT = 10;

    public static ResourceLocation asResource(String path) {
        return Compat.rl(MOD_ID, path);
    }
}
