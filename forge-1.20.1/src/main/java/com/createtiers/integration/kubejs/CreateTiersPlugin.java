package com.createtiers.integration.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KubeJS plugin for Create Tiers.
 * Allows users to register custom tiers via KubeJS scripts.
 * 
 * Example usage in KubeJS:
 * <pre>
 * // In server_scripts or startup_scripts/create_tiers.js
 * CreateTiers.registerTier('basic', 1, 256, 1024);
 * CreateTiers.registerTier('advanced', 2, 512, 2048, 0xFFAA00);
 * CreateTiers.registerTier('elite', 3, 1024, 4096, 0x55FF55, 'Elite Tier');
 * </pre>
 */
public class CreateTiersPlugin extends KubeJSPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers/KubeJS");
    
    @Override
    public void registerBindings(BindingsEvent event) {
        // Register the CreateTiers binding class for use in scripts
        event.add("CreateTiers", CreateTiersBinding.class);
    }
}
